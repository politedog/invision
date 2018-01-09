package invision

import com.squareup.moshi.Moshi
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import org.w3c.dom.Document
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.transform.OutputKeys
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.TransformerFactory

fun main(args: Array<String>) {
    if (!validateArgs(args)) { help(); return }

    val invisionPath = File(args[0]).absoluteFile.toString()
    val invisionJson = getStringFromFile(s=invisionPath + "/index_pretty.json")
    val invision = Moshi.Builder().build().adapter(Invision::class.java).fromJson(invisionJson)
    if (invision == null) { help(); return }

    val path = File(args[1]).absoluteFile.toString()
    val buildGradle = getStringFromFile(s=path+"/app/build.gradle")
    val packageName = getPackageFromBuildGradle(buildGradle)
    if (packageName == null || packageName.isEmpty()) { help(); return }

    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val manifest = db.parse(File(path + "/app/src/main/AndroidManifest.xml"))
    val activities = manifest.getElementsByTagName("activity")
    val application = manifest.getElementsByTagName("application").item(0)
    System.out.println(packageName)
    System.out.println("There are ${invision.hotspots.size} hotspots on ${invision.screens.size} screens!")
    val screenMap = HashMap<Int, String>()
    invision.screens.forEach {
        val className = "Invision" + it.name.replace(Regex("^.*B2B "), "").replace(Regex("[^A-Za-z]"), "") + "Activity"
        screenMap.put(it.id, className)
        System.out.println("Added ${className}, map has ${screenMap.size} entries")
    }
    invision.screens.forEach { screen ->
        val className = screenMap.get(screen.id)?:""
        val builder = ScreenClassBuilder(packageName, invision)
        builder.classMap = screenMap
        builder.screen = screen
        val output = File(path + "/app/src/main/java/")
        val fileSpec = builder.build()
        if (fileSpec == null) {
            System.out.println("Failed to create class ${className}")
        } else {
            fileSpec.writeTo(output)
        }
        if (!(0 .. activities.length-1).any {
            activities.item(it).attributes.getNamedItem("android:name").nodeValue.contains(className)?:false
        }) {
            val newActivity = manifest.createElement("activity")
            newActivity.setAttribute("android:name", ".invision."+className)
            if (screen.id == invision.project.homeScreenID) {
                val intentFilter = manifest.createElement("intent-filter")
                val action = manifest.createElement("action")
                action.setAttribute("android:name", "android.intent.action.MAIN")
                val category = manifest.createElement("category")
                category.setAttribute("android:name", "android.intent.category.LAUNCHER")
                intentFilter.appendChild(action)
                intentFilter.appendChild(category)
                newActivity.appendChild(intentFilter)
            }
            application.appendChild(newActivity)
        }
        Files.copy(File("${invisionPath}/${screen.imageUrl}").toPath(), File("${path}/app/src/main/res/drawable/inv${screen.id}.png").toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    writeNewManifest(manifest, path)
}

fun validateArgs(args: Array<String>) : Boolean {
    if (args.size < 2) {
        return false
    }
    if (args[0].isEmpty() || args[1].isEmpty()) {
        return false
    }
    try {
        if(!File(args[1]).exists()) {
            return false
        }
    } catch (e: Exception) {
        return false
    }
    try {
        if(!File(args[0]).exists()) {
            return false
        }
    } catch (e: Exception) {
        return false
    }
    return true
}

fun help() {
    System.out.println("java -jar invision.jar _path_to_invision_ _path_to_project_")
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

data class Hotspot (val height: Int, val width: Int, val x: Int, val y: Int, val screenID: Int, val targetScreenID: Int)

data class Project (val homeScreenID: Int)

data class Screen (val imageUrl: String, val height: Int, val width: Int, val id: Int, val name: String)


