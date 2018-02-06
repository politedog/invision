package invision

import com.squareup.moshi.Moshi
import org.w3c.dom.Node
import java.io.File

class AndroidApp(val path: String) {
    val buildGradle = (path + "/app/build.gradle").getStringFromFile()
    val packageName = getPackageFromBuildGradle(buildGradle)
    val invisionPackage = packageName + ".invision"
    val manifest = getDocBuilder().parse(File(path + "/app/src/main/AndroidManifest.xml"))
    val activities = manifest.getElementsByTagName("activity")
    val application = manifest.getElementsByTagName("application").item(0)
    var classMap = ClassMap()

    fun validate(): Boolean {
        return !packageName.isNullOrEmpty()
    }

    override fun toString(): String {
        val retval = packageName?:"[No package!]"
        return retval
    }

    fun addActivity(className: String, home: Boolean) {
        val newActivity = manifest.createElement("activity")
        newActivity.setAttribute("android:name", ".invision."+className)
        if (home) {
            newActivity.appendChild(getLauncherIntentFilter(manifest))
        }
        application.appendChild(newActivity)
    }

    fun  findActivityByClassName(className: String): Node? {
        val activityIdx = (0 .. activities.length-1).find {
            activities.item(it).attributes.getNamedItem("android:name").nodeValue.contains(className)?:false
        }
        if (activityIdx == null) {
            return null
        }
        return activities.item(activityIdx)
    }

    fun generateClassMap(invision: Invision) : Boolean {
        val moshi = Moshi.Builder().build()
        val mapFn = path + "/classmap.json"
        val mapFile = File(mapFn)
        val existingMapFile = mapFile.exists()
        if (existingMapFile) {
            classMap = moshi.adapter(ClassMap::class.java).fromJson(mapFn.getStringFromFile())?:classMap
        }
        invision.screens.forEach {
            val classMapping = classMap.get(it.id)
            val className = "Invision" + it.name.replace(Regex("^.*B2B "), "").replace(Regex("[^A-Za-z]"), "") + "Activity"
            if (classMapping != null) {
                if (classMapping.autoGenerated && classMapping.name != className) {
                    classMapping.name = className
                } else {
                    val activity = findActivityByClassName(classMapping.name)
                    var classPackageName = activity?.attributes?.getNamedItem("android:name")?.nodeValue?:""
                    classPackageName = classPackageName.replace(Regex("\\.[^.]*$"), "")
                    if (classPackageName.matches(Regex("^\\."))) {
                        classPackageName = packageName + classPackageName
                    }
                    classMapping.packageName = classPackageName
                }
            } else {
                classMap.classList.add(ClassMapping(id = it.id, name = className, packageName = invisionPackage))
            }
        }
        mapFile.writeText(moshi.adapter(ClassMap::class.java).indent("  ").toJson(classMap))
        return existingMapFile
    }
}