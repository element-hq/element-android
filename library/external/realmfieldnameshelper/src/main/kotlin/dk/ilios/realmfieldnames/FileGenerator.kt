package dk.ilios.realmfieldnames

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

/**
 * Class responsible for creating the final output files.
 */
class FileGenerator(private val filer: Filer) {
    private val formatter: FieldNameFormatter

    init {
        this.formatter = FieldNameFormatter()
    }

    /**
     * Generates all the "&lt;class&gt;Fields" fields with field name references.
     * @param fileData Files to create.
     * *
     * @return `true` if the files where generated, `false` if not.
     */
    fun generate(fileData: Set<ClassData>): Boolean {
        return fileData
                .filter { !it.libraryClass }
                .all { generateFile(it, fileData) }
    }

    private fun generateFile(classData: ClassData, classPool: Set<ClassData>): Boolean {
        val fileBuilder = TypeSpec.classBuilder(classData.simpleClassName + "Fields")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("This class enumerate all queryable fields in {@link \$L.\$L}\n",
                        classData.packageName, classData.simpleClassName)

        // Add a static field reference to each queryable field in the Realm model class
        classData.fields.forEach { fieldName, value ->
            if (value != null) {
                // Add linked field names (only up to depth 1)
                for (data in classPool) {
                    if (data.qualifiedClassName == value) {
                        val linkedTypeSpec = TypeSpec.classBuilder(formatter.format(fieldName))
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                        val linkedClassFields = data.fields
                        addField(linkedTypeSpec, "$", fieldName)
                        for (linkedFieldName in linkedClassFields.keys) {
                            addField(linkedTypeSpec, linkedFieldName, fieldName + "." + linkedFieldName)
                        }
                        fileBuilder.addType(linkedTypeSpec.build())
                    }
                }
            } else {
                // Add normal field name
                addField(fileBuilder, fieldName, fieldName)
            }
        }

        val javaFile = JavaFile.builder(classData.packageName, fileBuilder.build()).build()
        try {
            javaFile.writeTo(filer)
            return true
        } catch (e: IOException) {
            // e.printStackTrace()
            return false
        }
    }

    private fun addField(fileBuilder: TypeSpec.Builder, fieldName: String, fieldNameValue: String) {
        val field = FieldSpec.builder(String::class.java, formatter.format(fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("\$S", fieldNameValue)
                .build()
        fileBuilder.addField(field)
    }
}
