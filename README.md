# img2scad

Texture map images onto OpenSCAD surfaces, optionally rendering to STL.

Here is a preview of the planetary maps of the solar system found in [`images/1k/`](images/1k), converted to OpenSCAD, and then rendered to STL:

![solar-system](https://user-images.githubusercontent.com/102931/103391580-252af980-4b12-11eb-9809-95d871829265.png)

These STLs can be downloaded on [PrusaPrinters](https://www.prusaprinters.org/prints/50410-solar-system) or [Thingiverse](https://www.thingiverse.com/thing:4703616).

## Usage

Run:

`./img2scad.kts <jpg folder> <scad folder> <stl folder> <optional: openscad command>`

For example:

`./img2scad.kts "jpg" "scad" "stl" "flatpak run org.openscad.OpenSCAD"`

1. Pick up all images in the `jpg/` folder, generate OpenSCAD polyhedrons based on them, and write them to `scad/`.

	_Note: Planetary samples are provided in `images/.../`. Higher resolutions provide more details but are slower and resource intensive._

2. Export all OpenSCAD files in the `scad/` folder as STLs to `stl/`. This step is optional and only runs because the OpenSCAD command is specified.
	
	_Note: This step is slow and requires a lot of RAM, e.g., up to 15GB for earth in 1k resolution._

## Configuration

### Shapes

The code renders onto a sphere, though [`img2scad.kt`](img2scad.kt) can be adjusted to render onto just about any surface, provided you represent it mathematically. In the source code, an example is provided for a disk.

For any adjustment, review:
- `scad/model.scad-suffix`: By default, it provides a round base, which might not make sense for non-spherical shapes.
- `scad/*.scad` & `scad/*.scad-prefix`: By default, this separation is used to scale each planet diameter by `log2(diameter/1000)`, which you might not want.

### Texture

Increase or decrease the intensity of the texture by tweaking the `depth` argument of `writeScad()` in [`img2scad.kt`](img2scad.kt).

## Acknowledgements

[Texture Mapping OpenScad](https://www.thingiverse.com/thing:363298)'s processing program is the foundation for the code in this repository.

Sample images of the solar system are provided by [Solar System Scope](https://www.solarsystemscope.com/textures/).