/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.moshi.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.isInner
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@KotlinPoetMetadataPreview class JsonClassProcessor : AbstractProcessor() {

    private val annotation = JsonClass::class.java

    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var elements: Elements
    private lateinit var types: Types
    private lateinit var options: Map<String, String>
    private lateinit var classInspector: ClassInspector

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        messager = processingEnv.messager
        elements = processingEnv.elementUtils
        types = processingEnv.typeUtils
        options = processingEnv.options
        classInspector = ElementsClassInspector.create(elements, types)
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(annotation.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    @KotlinPoetMetadataPreview
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.errorRaised()) {
            return false
        }
        val jsonKeysByClass = HashMap<ClassName, Set<String>>()
        roundEnv.getElementsAnnotatedWith(annotation)
                .asSequence()
                .map { it as TypeElement }
                .forEach { type ->
                    val jsonClass = type.getAnnotation(JsonClass::class.java)
                    if (!jsonClass.generateAdapter) return@forEach
                    val kmClass = type.toImmutableKmClass()
                    if (!kmClass.isData) {
                        return@forEach
                    }
                    if (kmClass.isInner) {
                        return@forEach
                    }
                    val dataClassSpec = kmClass.toTypeSpec(classInspector)
                    val jsonKeys = dataClassSpec.primaryConstructor?.parameters?.map {
                        it.annotations.jsonName() ?: it.name
                    }.orEmpty().toSet()
                    val typeName: String = type.qualifiedName.toString()
                    val className = ClassName("", typeName)
                    jsonKeysByClass[className] = jsonKeys
                }

        val packageName = "org.matrix.android.sdk.internal"
        val generatedClassName = ClassName(packageName, "JsonKeys")
        val objectBuilder = TypeSpec.objectBuilder(generatedClassName).addModifiers(KModifier.INTERNAL)
        val setTypeName = Set::class.asClassName().parameterizedBy(String::class.asTypeName())
        val anyClassName = Class::class.asClassName().parameterizedBy(TypeVariableName("*"))
        val mapTypeName = Map::class.asClassName().parameterizedBy(anyClassName, setTypeName)

        val jsonKeysPropertySpec = PropertySpec.builder("jsonKeysByClasses", mapTypeName)
                .addModifiers(KModifier.PUBLIC)
                .initializer(
                        "mapOf(\n%L\n)",
                        jsonKeysByClass
                                .map {
                                    val setOfBlock = it.value.map { jsonKey ->
                                        CodeBlock.of("%S", jsonKey)
                                    }.joinToCode(", ")
                                    CodeBlock.of("%L::class.java to setOf(%L)", it.key, setOfBlock)
                                }.joinToCode(", \n")
                )
                .build()

        objectBuilder.addProperty(jsonKeysPropertySpec)

        try {
            val fileSpec = FileSpec.builder(packageName, "JsonKeys")
                    .addType(objectBuilder.build())
                    .build()
            fileSpec.writeTo(filer)
        } catch (e: IOException) {
            messager.printMessage(Diagnostic.Kind.NOTE, e.toString())
        }

        return true
    }

    private fun List<AnnotationSpec>?.jsonName(): String? {
        if (this == null) return null
        return find { it.typeName == Json::class.asClassName() }?.let { annotation ->
            val mirror = requireNotNull(annotation.tag<AnnotationMirror>()) {
                "Could not get the annotation mirror from the annotation spec"
            }
            mirror.elementValues.entries.single {
                it.key.simpleName.contentEquals("name")
            }.value.value as String
        }
    }
}
