package com.mangofactory.swagger.readers

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.mangofactory.swagger.configuration.SwaggerGlobalSettings
import com.mangofactory.swagger.mixins.RequestMappingSupport
import com.mangofactory.swagger.mixins.SwaggerPathProviderSupport
import com.mangofactory.swagger.models.configuration.SwaggerModelsConfiguration
import com.mangofactory.swagger.scanners.RequestMappingContext
import com.mangofactory.swagger.scanners.ResourceGroup
import com.wordnik.swagger.models.Path
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import spock.lang.Specification
import spock.lang.Unroll

@Mixin([RequestMappingSupport, SwaggerPathProviderSupport])
class ApiPathReaderSpec extends Specification {

  @Unroll
  def "should generate an api paths for each request mapping pattern"() {
    given:
      ApiPathReader apiPathReader = new ApiPathReader(pathProvider, [])
      RequestMappingInfo requestMappingInfo = requestMappingInfo("/doesNotMatterForThisTest",
              [patternsRequestCondition: patternsRequestCondition('/somePath/{businessId}',
                      '/somePath/another/{businessId:\\d+}')]
      )

      HandlerMethod handlerMethod = dummyHandlerMethod()
      RequestMappingContext context = new RequestMappingContext(requestMappingInfo, handlerMethod)

      def settings = new SwaggerGlobalSettings()
      SwaggerModelsConfiguration springSwaggerConfig = new SwaggerModelsConfiguration()
      settings.alternateTypeProvider = springSwaggerConfig.alternateTypeProvider();
      context.put("swaggerGlobalSettings", settings)
      context.put("currentResourceGroup", Mock(ResourceGroup))
    when:
      apiPathReader.execute(context)
      Map<String, Object> result = context.getResult()

    then:
      Map apiPathMap = result['swaggerPaths']
      apiPathMap.size() == 2

      apiPathMap["$prefix/somePath/{businessId}"]
      apiPathMap["$prefix/somePath/another/{businessId}"]

    and:
      String[] methods = Path.getAnnotation(JsonPropertyOrder).value()
      apiPathMap.each { key, val ->
        methods.each {
          assert val."${it}"
        }
      }

    where:
      pathProvider                  | prefix
      absoluteSwaggerPathProvider() | "/api/v1"
      relativeSwaggerPathProvider() | ""
  }

  def "should sanitize request mapping endpoints"() {
    expect:
      new ApiPathReader(absoluteSwaggerPathProvider(), []).sanitizeRequestMappingPattern(mappingPattern) ==
              expected

    where:
      mappingPattern             | expected
      ""                         | "/"
      "/"                        | "/"
      "/businesses"              | "/businesses"
      "/{businessId:\\w+}"       | "/{businessId}"
      "/businesses/{businessId}" | "/businesses/{businessId}"
      "/foo/bar:{baz}"           | "/foo/bar:{baz}"
      "/foo:{foo}/bar:{baz}"     | "/foo:{foo}/bar:{baz}"
      "/foo/bar:{baz:\\w+}"      | "/foo/bar:{baz}"

  }


}
