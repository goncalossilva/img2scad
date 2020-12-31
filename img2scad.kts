#!/usr/bin/env -S kotlinc -jvm-target 1.8 -script

// ./img2scad.main.kts jpg scad stl "flatpak run org.openscad.OpenSCAD"

import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.*

private val df = DecimalFormat.getInstance().apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 5
    roundingMode = RoundingMode.HALF_UP
}

fun Double.format(): String {
    val str = df.format(this)
    return if (str == "-0") "0" else str
}

fun brightness(color: Int): Double {
    fun Int.red() = (this shr 16 and 0xFF).toDouble()
    fun Int.green() = (this shr 8 and 0xFF).toDouble()
    fun Int.blue() = (this shr 0 and 0xFF).toDouble()

    return sqrt(
            0.299 * color.red().pow(2) + 0.587 * color.green().pow(2) + 0.114 * color.blue().pow(2)
    )
}

fun writeScad(
        /** Input image path. **/
        inPath: String,
        /** Output openscad path. **/
        outPath: String,
        /** Diameter of the resulting sphere. **/
        diameter: Double = 180.0,
        /** Depth. **/
        depth: Double = diameter * 0.04,
        /** Grid to average pixels, set to 1 for no decimation. **/
        grid: Int = 2
) {
    val image = ImageIO.read(File(inPath))
    val width = image.width
    val height = image.height

    val out = File(outPath).bufferedWriter()
    out.write("module body() {\n  polyhedron(\n    points = [\n")

    for (i in 0..height) {
        for (j in 0..width) {
            var brightness = 0.0
            for (ii in 0 until grid) {
                for (jj in 0 until grid) {
                    val (x, y) = when (i) {
                        0 -> Pair(0 + jj, 0)
                        height -> Pair(0 + jj, height - 1)
                        else -> Pair((j + jj) % width, min(i + ii, height - 1))
                    }
                    brightness += brightness(image.getRGB(x, y))
                }
            }
            brightness /= grid * grid * 255

            val r = diameter / 2
            val adjust = depth * brightness
            val theta = 2 * Math.PI * j / width
            val phi = Math.PI * i / height

            // Sphere
            val x = (r + adjust) * cos(theta) * sin(phi)
            val y = (r + adjust) * sin(theta) * sin(phi)
            val z = (r + adjust) * cos(phi)

            // Disk
            //val x = (1 + adjust) * sign(cos(theta) * sin(phi))
            //val y = (r + adjust) * sin(theta) * sin(phi)
            //val z = (r + adjust) * cos(phi)

            out.write("      ")
            out.write("[${x.format()}, ${y.format()}, ${z.format()}],\n")
        }
    }

    out.write("\n    ],\n    faces = [")

    for (i in 0..height) {
        for (j in 0..width) {
            out.write("      ")
            out.write("[${i * width + j}, ${(i + 1) * width + (j + 1)}, ${(i + 1) * width + j}], ")
            out.write("[${i * width + j}, ${i * width + (j + 1)}, ${(i + 1) * width + (j + 1)}],\n")
        }
    }
    out.write("    ],\n    convexity = 10);\n}")

    out.close()
}

fun writeStl(
        /** Openscad executable command. **/
        openscadCmd: String,
        /** Input openscad path. **/
        inPath: String,
        /** Output stl path. **/
        outPath: String
) {
    val cmd = "$openscadCmd $inPath -o $outPath"
    ProcessBuilder(*cmd.split(" ").toTypedArray())
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
}

val jpg = args.getOrNull(0) ?: "jpg"
val scad = args.getOrNull(1) ?: "scad"
val stl = args.getOrNull(2) ?: "stl"
val jpgFolder = File(jpg)
require(jpgFolder.exists()) { "jpg path is not a folder" }
val scadFolder = File(scad)
require(scadFolder.exists()) { "scad path is not a folder" }
val stlFolder = File(stl)
require(stlFolder.exists()) { "stl path is not a folder" }
val openscadCmd = args.getOrNull(3)

jpgFolder.walk().forEach {
    println(it)
    if (it.isFile && Files.probeContentType(it.toPath())?.startsWith("image/") == true) {
        val imgPath = it.path
        val scadPrefixPath = "$scadFolder/${it.name.replaceAfterLast(".", "scad-prefix")}"
        val scadPath = "$scadFolder/${it.name.replaceAfterLast(".", "scad")}"
        val stlPath = "$stlFolder/${it.name.replaceAfterLast(".", "stl")}"

        println("Converting $imgPath to $scadPrefixPath...")
        writeScad(imgPath, scadPrefixPath)

        if (openscadCmd != null) {
            println("Converting $scadPath to $stlPath...")
            writeStl(openscadCmd, scadPath, stlPath)
        }
    }
}

