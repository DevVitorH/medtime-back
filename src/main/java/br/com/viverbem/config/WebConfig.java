package br.com.viverbem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor auth;
  private final String[] origens;

  public WebConfig(AuthInterceptor auth, @Value("${app.cors.origins}") String[] origens) {
    this.auth = auth; this.origens = origens;
  }

  @Override
  public void addCorsMappings(CorsRegistry r) {
    r.addMapping("/api/**").allowedOrigins(origens)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*");
  }

  @Override
  public void addInterceptors(InterceptorRegistry r) {
    r.addInterceptor(auth).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**");
  }
}
