package dk.ilios.realmfieldnames

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

/**
 * The Realm Field Names Generator is a processor that looks at all available Realm model classes
 * and create an companion class with easy, type-safe access to all field names.
 */

@SupportedAnnotationTypes("io.realm.annotations.RealmClass")
class RealmFieldNamesProcessor : AbstractProcessor() {

    private val classes = HashSet<ClassData>()
    private lateinit var typeUtils: Types
    private lateinit var messager: Messager
    private lateinit var elementUtils: Elements
    private var ignoreAnnotation: TypeMirror? = null
    private var realmClassAnnotation: TypeElement? = null
    private var realmModelInterface: TypeMirror? = null
    private var realmListClass: DeclaredType? = null
    private var realmResultsClass: DeclaredType? = null
    private var fileGenerator: FileGenerator? = null
    private var done = false

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        messager = processingEnv.messager!!
        elementUtils = processingEnv.elementUtils!!

        // If the Realm class isn't found something is wrong the project setup.
        // Most likely Realm isn't on the class path, so just disable the
        // annotation processor
        val isRealmAvailable = elementUtils.getTypeElement("io.realm.Realm") != null
        if (!isRealmAvailable) {
            done = true
        } else {
            ignoreAnnotation = elementUtils.getTypeElement("io.realm.annotations.Ignore")?.asType()
            realmClassAnnotation = elementUtils.getTypeElement("io.realm.annotations.RealmClass")
            realmModelInterface = elementUtils.getTypeElement("io.realm.RealmModel")?.asType()
            realmListClass = typeUtils.getDeclaredType(
                    elementUtils.getTypeElement("io.realm.RealmList"),
                    typeUtils.getWildcardType(null, null)
            )
            realmResultsClass = typeUtils.getDeclaredType(
                    elementUtils.getTypeElement("io.realm.RealmResults"),
                    typeUtils.getWildcardType(null, null)
            )
            fileGenerator = FileGenerator(processingEnv.filer)
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (done) {
            return CONSUME_ANNOTATIONS
        }

        // Create all proxy classes
        roundEnv.getElementsAnnotatedWith(realmClassAnnotation).forEach { classElement ->
            if (typeUtils.isAssignable(classElement.asType(), realmModelInterface)) {
                val classData = processClass(classElement as TypeElement)
                classes.add(classData)
            }
        }

        // If a model class references a library class, the library class will not be part of this
        // annotation processor round. For all those references we need to pull field information
        // from the classpath instead.
        val libraryClasses = HashMap<String, ClassData>()
        classes.forEach {
            it.fields.forEach { _, value ->
                // Analyze the library class file the first time it is encountered.
                if (value != null) {
                    if (classes.all { it.qualifiedClassName != value } && !libraryClasses.containsKey(value)) {
                        libraryClasses.put(value, processLibraryClass(value))
                    }
                }
            }
        }
        classes.addAll(libraryClasses.values)

        done = fileGenerator!!.generate(classes)
        return CONSUME_ANNOTATIONS
    }

    private fun processClass(classElement: TypeElement): ClassData {
        val packageName = getPackageName(classElement)
        val className = classElement.simpleName.toString()
        val data = ClassData(packageName, className)

        // Find all appropriate fields
        classElement.enclosedElements.forEach {
            val elementKind = it.kind
            if (elementKind == ElementKind.FIELD) {
                val variableElement = it as VariableElement

                val modifiers = variableElement.modifiers
                if (modifiers.contains(Modifier.STATIC)) {
                    return@forEach // completely ignore any static fields
                }

                // Don't add any fields marked with @Ignore
                val ignoreField = variableElement.annotationMirrors
                        .map { it.annotationType.toString() }
                        .contains("io.realm.annotations.Ignore")

                if (!ignoreField) {
                    data.addField(it.getSimpleName().toString(), getLinkedFieldType(it))
                }
            }
        }

        return data
    }

    private fun processLibraryClass(qualifiedClassName: String): ClassData {
        val libraryClass = Class.forName(qualifiedClassName) // Library classes should be on the classpath
        val packageName = libraryClass.`package`.name
        val className = libraryClass.simpleName
        val data = ClassData(packageName, className, libraryClass = true)

        libraryClass.declaredFields.forEach { field ->
            if (java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                return@forEach // completely ignore any static fields
            }

            // Add field if it is not being ignored.
            if (field.annotations.all { it.toString() != "io.realm.annotations.Ignore" }) {
                data.addField(field.name, field.type.name)
            }
        }

        return data
    }

    /**
     * Returns the qualified name of the linked Realm class field or `null` if it is not a linked
     * class.
     */
    private fun getLinkedFieldType(field: Element): String? {
        if (typeUtils.isAssignable(field.asType(), realmModelInterface)) {
            // Object link
            val typeElement = elementUtils.getTypeElement(field.asType().toString())
            return typeElement.qualifiedName.toString()
        } else if (typeUtils.isAssignable(field.asType(), realmListClass) || typeUtils.isAssignable(field.asType(), realmResultsClass)) {
            // List link or LinkingObjects
            val fieldType = field.asType()
            val typeArguments = (fieldType as DeclaredType).typeArguments
            if (typeArguments.size == 0) {
                return null
            }
            return typeArguments[0].toString()
        } else {
            return null
        }
    }

    private fun getPackageName(classElement: TypeElement): String? {
        val enclosingElement = classElement.enclosingElement

        if (enclosingElement.kind != ElementKind.PACKAGE) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not determine the package name. Enclosing element was: " + enclosingElement.kind
            )
            return null
        }

        val packageElement = enclosingElement as PackageElement
        return packageElement.qualifiedName.toString()
    }

    companion object {
        private const val CONSUME_ANNOTATIONS = false
    }
}
