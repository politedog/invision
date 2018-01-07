package invision

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.squareup.kotlinpoet.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import com.sun.xml.internal.ws.addressing.EndpointReferenceUtil.transform
import org.w3c.dom.Document
import javax.xml.transform.OutputKeys
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory



fun main(args: Array<String>) {
    val invisionJson = getStringFromFile(s=File(args[0]).absoluteFile.toString() + "/index_pretty.json")
    val invision = Moshi.Builder().build().adapter(Invision::class.java).fromJson(invisionJson)
    invision?:return

    val path = File(args[1]).absoluteFile.toString()
    val buildGradle = getStringFromFile(s=path+"/app/build.gradle")
    val packageName = getPackageFromBuildGradle(buildGradle)
    packageName?:return

    val invisionPackage = packageName + ".invision"
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val manifest = db.parse(File(path + "/app/src/main/AndroidManifest.xml"))
    val activities = manifest.getElementsByTagName("activity")
    val application = manifest.getElementsByTagName("application").item(0)
    System.out.println(packageName)
    System.out.println("There are ${invision.hotspots.size} hotspots on ${invision.screens.size} screens!")
    invision.screens.forEach {
        val className = it.name.replace(Regex("^.*B2B "), "").replace(" ", "") + "Activity"
        val viewClassName = ClassName("android.widget", "View")
        val bundleClassName = ClassName("android.os", "Bundle")
        val imageViewClassName = ClassName("android.widget", "ImageView")
        System.out.println(className)
        val screenClassName = ClassName(invisionPackage, className)
        val baseClassName = ClassName(invisionPackage, "BaseInvisionActivity")

        val screenClass = TypeSpec.classBuilder(className)
                .superclass(baseClassName)
                .addProperty(PropertySpec.builder("wire", imageViewClassName).mutable(true).addModifiers(KModifier.LATEINIT).build())
                .addFunction(FunSpec.builder("onCreate")
                        .addParameter("savedInstanceState", bundleClassName.asNullable())
                        .addModifiers(KModifier.OVERRIDE)
                        .addStatement("super.onCreate(savedInstanceState)", "savedInstanceState")
                        .addStatement("setContentView(%L)", "R.layout.invision_activity")
                        .addStatement("%L=findViewById(%L)", "wire", "R.id.wire")
                        .beginControlFlow("%L.setOnTouchListener", "wire")
                        .addStatement("view, motionEvent -> ")
                        .beginControlFlow("%L.forEach","hotspots")
                        .beginControlFlow("if(it.contains(view, motionEvent))")
                        .addStatement("val i = Intent(this, it.target)")
                        .addStatement("startActivity(i)")
                        .endControlFlow()
                        .endControlFlow()
                        .build())
                .build()
        val fileSpec = FileSpec.builder(invisionPackage, className)
                .addType(screenClass)
                .addStaticImport(packageName, "R")
                .build()
        val output = File(path + "/app/src/main/java/")
        fileSpec.writeTo(output)
        if (!(0 .. activities.length-1).any {
            activities.item(it).attributes.getNamedItem("android:name").nodeValue.contains(className)?:false
        }) {
            val newActivity = manifest.createElement("activity")
            newActivity.setAttribute("android:name", ".invision."+className)
            application.appendChild(newActivity)
        }
        writeNewManifest(manifest, path)
    }
}

fun  getPackageFromBuildGradle(buildGradle: String): String? {
    val appIdPattern = Pattern.compile("""applicationId\s*(["'`])([^"'`]*)\1""")
    val appIdMatcher = appIdPattern.matcher(buildGradle)
    if (appIdMatcher.find()) else return null
    return appIdMatcher.group(2)
}

private fun writeNewManifest(manifest: Document?, path: String) {
    val ds = DOMSource(manifest)
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    val result = StreamResult(path + "/app/src/main/NewAndroidManifest.xml")
    transformer.transform(ds, result)
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


