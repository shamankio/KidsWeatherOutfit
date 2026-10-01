#!/usr/bin/env swift
// Generates the adaptive-icon foreground layers (all densities), the 512 px Play Store icon
// and a mask preview from design/icon/icon_master.jpeg.
//
// Run from the repo root:  swift tools/icon/make_icons.swift
//
// macOS only (uses Vision for the subject cut-out, CoreGraphics/ImageIO for rendering).

import CoreGraphics
import CoreImage
import Foundation
import ImageIO
import UniformTypeIdentifiers
import Vision

// MARK: - Config

let masterPath = "design/icon/icon_master.jpeg"
let cutoutPath = "design/icon/foreground_master.png"
let previewPath = "design/icon/preview.png"
let playStorePath = "app/src/main/ic_launcher-playstore.png"
let resDir = "app/src/main/res"

let canvasDp = 108.0      // adaptive icon layer size
let visibleDp = 72.0      // area the launcher shows before masking
let safeDp = 66.0         // content that must never be cropped by any mask (circle diameter)
let artRadiusDp = 32.0    // art is scaled so its enclosing circle has this radius (1 dp inside the safe zone)

// Sky blue seed colour from ui/theme/Color.kt (#4FC3F7).
let background = (r: 0x4F, g: 0xC3, b: 0xF7)

let densities: [(name: String, scale: Double)] = [
    ("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4),
]

// MARK: - Helpers

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("error: \(message)\n".utf8))
    exit(1)
}

let srgb = CGColorSpace(name: CGColorSpace.sRGB)!

func makeContext(_ width: Int, _ height: Int) -> CGContext {
    guard let ctx = CGContext(
        data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
        space: srgb, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else { fail("cannot create \(width)x\(height) bitmap context") }
    ctx.interpolationQuality = .high
    return ctx
}

func loadImage(_ path: String) -> CGImage {
    guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: path) as CFURL, nil),
          let image = CGImageSourceCreateImageAtIndex(source, 0, nil)
    else { fail("cannot read \(path) (run from the repo root)") }
    return image
}

func writePNG(_ image: CGImage, to path: String) {
    let url = URL(fileURLWithPath: path)
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil) else {
        fail("cannot create \(path)")
    }
    CGImageDestinationAddImage(dest, image, nil)
    guard CGImageDestinationFinalize(dest) else { fail("cannot write \(path)") }
    print("wrote \(path) (\(image.width)x\(image.height))")
}

func fillBackground(_ ctx: CGContext, _ rect: CGRect) {
    ctx.setFillColor(CGColor(
        red: CGFloat(background.r) / 255, green: CGFloat(background.g) / 255,
        blue: CGFloat(background.b) / 255, alpha: 1
    ))
    ctx.fill(rect)
}

// MARK: - Subject cut-out

/// Lifts the subject (the shirt) off the baked-in gradient background -> RGBA with transparency.
func cutOutSubject(_ image: CGImage) -> CGImage {
    let request = VNGenerateForegroundInstanceMaskRequest()
    let handler = VNImageRequestHandler(cgImage: image)
    do {
        try handler.perform([request])
        guard let observation = request.results?.first else { fail("Vision found no foreground subject") }
        let buffer = try observation.generateMaskedImage(
            ofInstances: observation.allInstances, from: handler, croppedToInstancesExtent: false
        )
        let ci = CIImage(cvPixelBuffer: buffer)
        guard let out = CIContext().createCGImage(ci, from: ci.extent, format: .RGBA8, colorSpace: srgb) else {
            fail("cannot convert the masked image")
        }
        return out
    } catch {
        fail("Vision request failed: \(error)")
    }
}

/// Centre of the opaque bounding box and the radius of the smallest circle around that centre
/// that contains every opaque pixel. Coordinates are image pixels, y growing downwards.
func measure(_ image: CGImage) -> (cx: Double, cy: Double, radius: Double) {
    let w = image.width, h = image.height
    let ctx = makeContext(w, h)
    ctx.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))
    let bytes = ctx.data!.assumingMemoryBound(to: UInt8.self)
    let stride = ctx.bytesPerRow

    var minX = w, maxX = -1, minY = h, maxY = -1
    for y in 0..<h {
        for x in 0..<w where bytes[y * stride + x * 4 + 3] > 127 {
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)
        }
    }
    guard maxX >= 0 else { fail("cut-out is fully transparent") }

    let cx = Double(minX + maxX) / 2, cy = Double(minY + maxY) / 2
    var maxDist2 = 0.0
    for y in 0..<h {
        for x in 0..<w where bytes[y * stride + x * 4 + 3] > 127 {
            let dx = Double(x) - cx, dy = Double(y) - cy
            maxDist2 = max(maxDist2, dx * dx + dy * dy)
        }
    }
    return (cx, cy, maxDist2.squareRoot())
}

// MARK: - Rendering

/// Square transparent layer of `size` px where the art's centre is at the middle and its enclosing
/// circle has radius `artRadiusDp * pxPerDp`.
func renderForeground(_ cut: CGImage, _ m: (cx: Double, cy: Double, radius: Double), size: Int, pxPerDp: Double) -> CGImage {
    let ctx = makeContext(size, size)
    let scale = artRadiusDp * pxPerDp / m.radius
    let cgCy = Double(cut.height) - m.cy   // memory rows are top-down, CG space is bottom-up
    ctx.draw(cut, in: CGRect(
        x: Double(size) / 2 - m.cx * scale,
        y: Double(size) / 2 - cgCy * scale,
        width: Double(cut.width) * scale,
        height: Double(cut.height) * scale
    ))
    return ctx.makeImage()!
}

func composite(_ foreground: CGImage) -> CGImage {
    let size = foreground.width
    let ctx = makeContext(size, size)
    let rect = CGRect(x: 0, y: 0, width: size, height: size)
    fillBackground(ctx, rect)
    ctx.draw(foreground, in: rect)
    return ctx.makeImage()!
}

// MARK: - Mask preview

func superellipse(center: CGPoint, radius: CGFloat, exponent: Double = 4) -> CGPath {
    let path = CGMutablePath()
    let steps = 360
    for i in 0...steps {
        let t = Double(i) / Double(steps) * 2 * .pi
        let c = cos(t), s = sin(t)
        let x = pow(abs(c), 2 / exponent) * (c < 0 ? -1 : 1)
        let y = pow(abs(s), 2 / exponent) * (s < 0 ? -1 : 1)
        let p = CGPoint(x: center.x + CGFloat(x) * radius, y: center.y + CGFloat(y) * radius)
        i == 0 ? path.move(to: p) : path.addLine(to: p)
    }
    path.closeSubpath()
    return path
}

func renderPreview(_ full: CGImage) {
    let pxPerDp = 4.0
    let panel = Int(canvasDp * pxPerDp)
    let gap = 24
    let names = 4
    let width = panel * names + gap * (names + 1)
    let height = panel + gap * 2
    let ctx = makeContext(width, height)
    ctx.setFillColor(CGColor(gray: 0.86, alpha: 1))
    ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))

    let half = visibleDp / 2 * pxPerDp
    let masks: [(CGPoint) -> CGPath?] = [
        { _ in nil },
        { c in CGPath(ellipseIn: CGRect(x: c.x - half, y: c.y - half, width: half * 2, height: half * 2), transform: nil) },
        { c in superellipse(center: c, radius: half) },
        { c in CGPath(roundedRect: CGRect(x: c.x - half, y: c.y - half, width: half * 2, height: half * 2),
                      cornerWidth: half * 0.4, cornerHeight: half * 0.4, transform: nil) },
    ]

    for (i, makeMask) in masks.enumerated() {
        let origin = CGPoint(x: gap + i * (panel + gap), y: gap)
        let rect = CGRect(origin: origin, size: CGSize(width: panel, height: panel))
        let center = CGPoint(x: rect.midX, y: rect.midY)
        ctx.saveGState()
        if let mask = makeMask(center) { ctx.addPath(mask); ctx.clip() }
        ctx.draw(full, in: rect)
        ctx.restoreGState()

        // Guides: 66 dp safe circle (red), 72 dp visible box (blue, first panel only).
        ctx.setLineWidth(2)
        ctx.setLineDash(phase: 0, lengths: [10, 8])
        ctx.setStrokeColor(CGColor(red: 0.9, green: 0.1, blue: 0.1, alpha: 0.9))
        let safe = safeDp / 2 * pxPerDp
        ctx.strokeEllipse(in: CGRect(x: center.x - safe, y: center.y - safe, width: safe * 2, height: safe * 2))
        if i == 0 {
            ctx.setStrokeColor(CGColor(red: 0.1, green: 0.2, blue: 0.9, alpha: 0.9))
            ctx.stroke(CGRect(x: center.x - half, y: center.y - half, width: half * 2, height: half * 2))
        }
        ctx.setLineDash(phase: 0, lengths: [])
    }
    writePNG(ctx.makeImage()!, to: previewPath)
}

// MARK: - Main

let master = loadImage(masterPath)
print("master: \(master.width)x\(master.height)")

let cut = cutOutSubject(master)
writePNG(cut, to: cutoutPath)

let m = measure(cut)
let artWidthDp = artRadiusDp * 2
print(String(format: "art centre (%.0f, %.0f) px, enclosing radius %.0f px -> %.0f dp circle (safe zone %.0f dp)",
             m.cx, m.cy, m.radius, artWidthDp, safeDp))

// Foreground layers: 108 dp at each density.
for density in densities {
    let size = Int((canvasDp * density.scale).rounded())
    let layer = renderForeground(cut, m, size: size, pxPerDp: density.scale)
    writePNG(layer, to: "\(resDir)/drawable-\(density.name)/ic_launcher_foreground.png")
}

// Play Store icon: the 72 dp the launcher shows, on the solid background, no baked-in mask or shadow.
let playForeground = renderForeground(cut, m, size: 512, pxPerDp: 512 / visibleDp)
writePNG(composite(playForeground), to: playStorePath)

// Preview of the full 108 dp canvas under a few masks.
let previewSize = Int(canvasDp * 4)
renderPreview(composite(renderForeground(cut, m, size: previewSize, pxPerDp: 4)))
