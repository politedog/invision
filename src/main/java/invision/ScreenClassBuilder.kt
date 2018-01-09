package invision

import com.squareup.kotlinpoet.*

class ScreenClassBuilder (val packageName: String, val invision: Invision) {
    companion object {
        val bundleClassName = ClassName("android.os", "Bundle")
    }

    val invisionPackage = packageName + ".invision"
    val baseClassName = ClassName(invisionPackage, "BaseInvisionActivity")
    var screen: Screen? = null
    var className: String? = null
    var classMap: HashMap<Int, String>? = null

    fun build() : FileSpec? {
        val screen = screen?:return null
        val classMap = classMap?:return null
        val className = classMap.get(screen.id)
        if (className == null) {
            System.out.println("${screen.id} not found in class map")
            return null
        }
        System.out.println("Preparing screen ${screen?.name}")
        val hotspotClassName = ClassName(invisionPackage, "Array<Hotspot>")
        var hotspotInit = "arrayOf("
        var skipFirst = true
        invision.hotspots.forEach { spot ->
            if (spot.screenID == screen?.id) {
                if (classMap?.get(spot.targetScreenID) != null) {
                    if (skipFirst) skipFirst = false else hotspotInit += ",\n"
                    hotspotInit += "Hotspot(x=${1.0 * spot.x / screen.width}, y=${1.0 * spot.y / screen.height}, height=${1.0 * spot.height / screen.height}, width=${1.0 * spot.width / screen.width}, target=${classMap.get(spot.targetScreenID)}::class.java)"
                }
            }
        }
        hotspotInit += ");"

        val screenClass = TypeSpec.classBuilder(className)
                .superclass(baseClassName)
                .addProperty(PropertySpec.builder("hotspots", hotspotClassName).initializer(hotspotInit).build())
                .addFunction(FunSpec.builder("onCreate")
                        .addParameter("savedInstanceState", bundleClassName.asNullable())
                        .addModifiers(KModifier.OVERRIDE)
                        .addStatement("super.onCreate(savedInstanceState)", "savedInstanceState")
                        .addStatement("%L = %L", "bgResId", "R.drawable.inv${screen.id}")
                        .beginControlFlow("%L.setOnTouchListener", "wire")
                        .addStatement("view, motionEvent -> ")
                        .beginControlFlow("%L.forEach","hotspots")
                        .beginControlFlow("if(it.contains(view, motionEvent))")
                        .addStatement("val i = Intent(this, it.target)")
                        .addStatement("startActivity(i)")
                        .addStatement("true")
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("false")
                        .endControlFlow()
                        .build())
                .build()
        val fileSpec = FileSpec.builder(invisionPackage, className)
                .addType(screenClass)
                .addStaticImport(packageName?:"", "R")
                .addStaticImport("android.content", "Intent")
                .build()
        return fileSpec
    }

}

