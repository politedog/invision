package invision

import com.squareup.kotlinpoet.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File
import java.util.regex.Pattern

fun main(args: Array<String>) {
    val invisionJson = getStringFromFile(s=File(args[0]).absoluteFile.toString() + "/index_pretty.json")

    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(Invision::class.java)
    val invision = adapter.fromJson(invisionJson)
    invision?:return

    val buildGradle = getStringFromFile(s=File(args[1]).absoluteFile.toString()+"/app/build.gradle")
    val appIdPattern = Pattern.compile("""applicationId\s*(["'`])([^"'`]*)\1""")
    val appIdMatcher = appIdPattern.matcher(buildGradle)
    if (appIdMatcher.find()) else return
    val packageName = appIdMatcher.group(2)
    val invisionPackage = packageName + ".invision"
    System.out.println(packageName)
    System.out.println("There are ${invision.hotspots.size} hotspots on ${invision.screens.size} screens!")
    invision.screens.forEach {
        val className = it.name.replace(Regex("^.*B2B "), "").replace(" ", "") + "Activity"
        val bundleClassName = ClassName("android.os", "Bundle")
        System.out.println(className)
        val screenClassName = ClassName(invisionPackage, className)
        val baseClassName = ClassName(invisionPackage, "BaseInvisionActivity")
        val fileBuilder = FileSpec.builder(invisionPackage, className)

        val screenClass = TypeSpec.classBuilder(className)
                .superclass(baseClassName)
                .addFunction(FunSpec.builder("onCreate")
                        .addParameter("savedInstanceState", bundleClassName.asNullable())
                        .addModifiers(KModifier.OVERRIDE)
                        .addStatement("super.onCreate(%L)", "savedInstanceState")
                        .addStatement("setContentView(%L)", "R.layout.invision_activity")
                        .addStatement("")
                        .build())
                .build()
        val file = FileSpec.builder(invisionPackage, className)
                .addType(screenClass)
                .addStaticImport(packageName, "R")
                .build()
        file.writeTo(System.out)

    }
/*
    val greeterClass = ClassName("", args[0])
    val file = FileSpec.builder("", "HelloWorld")
            .addType(TypeSpec.classBuilder(args[0])
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("name", String::class)
                            .build())
                    .addProperty(PropertySpec.builder("name", String::class)
                            .initializer("name")
                            .build())
                    .addFunction(FunSpec.builder("greet")
                            .addStatement("println(%S)", "Hello, \$name")
                            .build())
                    .build())
            .addFunction(FunSpec.builder("main")
                    .addParameter("args", String::class, KModifier.VARARG)
                    .addStatement("%T(args[0]).greet()", greeterClass)
                    .build())
            .build()
*/
    println("Hello, old gradle file!")
}

fun  getStringFromFile(s: String): String {
    val jsonFile = File(s)
    val stream = jsonFile.inputStream()
    val string = stream.bufferedReader().use {
        it.readText()
    }
    return string
}

data class Invision (val hotspots: Array<Hotspot>, val project: Project, val screens: Array<Screen>)

data class Hotspot (val height: Int, val width: Int, val x: Int, val y: Int, val screenId: Int, val targetScreenId: Int)

data class Project (val homeScreenId: Int)

data class Screen (val imageUrl: String, val height: Int, val width: Int, val id: Int, val name: String)
