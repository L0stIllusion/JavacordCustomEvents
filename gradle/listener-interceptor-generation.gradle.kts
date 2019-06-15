import org.javacord.api.listener.GloballyAttachableListener
import org.reflections.Reflections
import com.squareup.kotlinpoet.*
import org.javacord.api.event.Event
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.function.BiConsumer

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.squareup:kotlinpoet:1.3.0")
        classpath("org.reflections:reflections:0.9.11")
        classpath("org.javacord:javacord:3.0.4")
    }
}

val outputDir = "$buildDir/generated/"

task("generateListenerInterceptors") {
    val listenerBase: Class<GloballyAttachableListener> = GloballyAttachableListener::class.java
    var interceptorsClassBuilder =
        FileSpec.builder("net.lostillusion.frameworks.javacordcustomevents", "JavacordListeners")
            .addImport(Pair::class, "")
            .addImport(ClassName("net.lostillusion.frameworks.javacordcustomevents", "CustomEvents"), "")
    //CREATE BASE LISTENER
    val baseInterceptor = TypeSpec.classBuilder("JavacordBaseListener")
        .addModifiers(KModifier.ABSTRACT)
        .addSuperinterface(listenerBase)
        .addTypeVariable(TypeVariableName("E", Event::class.java))
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("eventClass", Class::class.asClassName().parameterizedBy(TypeVariableName("E")))
                .build())
        .addProperty(
            PropertySpec
                .builder("eventClass", Class::class.asClassName().parameterizedBy(TypeVariableName("E")))
                .initializer("eventClass")
                .addModifiers(KModifier.PRIVATE)
                .build())
    val onEventFunction = FunSpec.builder("onEvent")
        .addParameter(ParameterSpec.builder("event", TypeVariableName("E")).build())
        .addCode("CustomEvents.dispatchers.filter { it.inputEventClass == eventClass }.mapNotNull { it as? CustomEventDispatcher<E, *> }.forEach { it.dispatchNormalEvent(event) }\n")
    baseInterceptor.addFunction(onEventFunction.build())
    interceptorsClassBuilder
        .addType(baseInterceptor.build())
    val JavacordBaseListener = ClassName("net.lostillusion.frameworks.javacordcustomevents", "JavacordBaseListener")
    //GET ALL LISTENERS
    val reflections = Reflections("org.javacord.api.listener")
    val javacordListeners = reflections.getSubTypesOf(listenerBase)
    //GENERATE LISTENERS
    val internalInterceptorsMap: MutableMap<Class<out GloballyAttachableListener>, TypeName> = mutableMapOf()
    javacordListeners.forEach {
        val eventMethod = it.methods
            .first()
        val eventType = eventMethod.parameterTypes[0]
        val interceptorName = "Javcord"+it.simpleName.replaceFirst("Listener", "Interceptor")
        val interceptor = TypeSpec.classBuilder(interceptorName)
            .superclass(JavacordBaseListener.parameterizedBy(eventType.asClassName()) )
            .addSuperclassConstructorParameter("%T::class.java", eventType)
            .addSuperinterface(it)
            .addFunction(
                FunSpec.builder(eventMethod.name)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("event", eventType)
                    .addStatement("onEvent(event)")
                    .build())
            .build()
        interceptorsClassBuilder.addType(interceptor)
        internalInterceptorsMap.put(it, ClassName("net.lostillusion.frameworks.javacordcustomevents", interceptorName))
    }
    val initText = StringBuilder()
        .append("mutableMapOf(")
    val initArgsMap: MutableMap<TypeName, TypeName> = mutableMapOf()
    internalInterceptorsMap
        .forEach(BiConsumer { listener, interceptor ->
            initText.append("Pair(%T::class.java, %T()),")
            initArgsMap.put(listener.asTypeName(), interceptor)
        })
    initText.deleteCharAt(initText.length-1)
    initText.append(")")
    val initArgs: ArrayList<TypeName> = arrayListOf()
    initArgsMap.toList()
        .forEach {
            initArgs.add(it.first)
            initArgs.add(it.second)
        }
    val interceptorMap =
        PropertySpec.builder("interceptorMap",
            Map::class.asClassName()
                .parameterizedBy(
                    Class::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(GloballyAttachableListener::class)),
                    JavacordBaseListener.parameterizedBy(WildcardTypeName.producerOf(Event::class))
                ))
            .initializer(CodeBlock.of(initText.toString(), *initArgs.toArray(arrayOf<TypeName>())))
            .build()
    //BUILD THE FUCKING CLASS
    interceptorsClassBuilder
        .addProperty(interceptorMap)
        .build()
        .writeTo(File(outputDir))
}
