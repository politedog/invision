package invision

import com.squareup.kotlinpoet.*

class ScreenClassBuilder (val packageName: String, val invision: Invision) {
    companion object {
        val bundleClassName = ClassName("android.os", "Bundle")
    }

    val invisionPackage = packageName + ".invision"
    val baseClassName = ClassName(invisionPackage, "BaseInvisionActivity")
    var screen: Screen? = null
    var classMap: ClassMap? = null

    fun setScreen(screen: Screen) : ScreenClassBuilder {
        this.screen = screen
        return this
    }

    fun setClassMap(classMap: ClassMap) : ScreenClassBuilder {
        this.classMap = classMap
        return this
    }

    fun build() : FileSpec? {
        val screen = screen?:return null
        val classMap = classMap?:return null
        val className = classMap.getName(screen.id)
        if (className == "") {
            System.out.println("${screen.id} not found in class map")
            return null
        }
        System.out.println("Preparing screen ${screen.name}")
        val hotspotClassName = ClassName(invisionPackage, "Array<Hotspot>")
        var hotspotInit = "arrayOf("
        var skipFirst = true
        val importSet = mutableSetOf<ClassMapping>()
        invision.hotspots.forEach { spot ->
            if (spot.screenID == screen.id) {
                val classMapping = classMap.get(spot.targetScreenID)
                if (classMapping != null) {
                    if (skipFirst) skipFirst = false else hotspotInit += ",\n"
                    hotspotInit += "Hotspot(x=${1.0 * spot.x / screen.width}, y=${1.0 * spot.y / screen.height}, height=${1.0 * spot.height / screen.height}, width=${1.0 * spot.width / screen.width}, target=${classMap.getName(spot.targetScreenID)}::class.java)"
                    importSet.add(classMapping)
                }
            }
        }
        hotspotInit += ");"

        val contextClassName = ClassName("android.content", "Context")
        val intentClassName = ClassName("android.content", "Intent")
        val companion = TypeSpec.companionObjectBuilder()
                .addFunction(FunSpec.builder("newIntent")
                        .addParameter("context", contextClassName)
                        .addParameter(ParameterSpec.builder("dummy", Any::class.asTypeName(), KModifier.VARARG).build())
                        .returns(intentClassName)
                        .addStatement("return Intent(context, %L::class.java)", className)
                        .build())
                .build()
        val screenClass = TypeSpec.classBuilder(className)
                .superclass(baseClassName)
                .addProperty(PropertySpec.builder("hotspots", hotspotClassName).initializer(hotspotInit).build())
                .addType(companion)
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
        importSet.forEach {
            if (!it.packageName.isNullOrEmpty()) {
                fileSpec.addStaticImport((packageName ?: "") + it.packageName, it.name)
            }
        }

        return fileSpec.build()
    }

}

