package ec.edu.unl.geoasist.identidades.presentacion;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JsonContractConfig {
  @Bean
  JsonMapperBuilderCustomizer strictContractTypes() {
    return builder -> {
      builder.withCoercionConfig(
          LogicalType.Textual,
          c -> {
            c.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
          });
      builder.withCoercionConfig(
          LogicalType.Integer,
          c -> {
            c.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
          });
      builder.withCoercionConfig(
          LogicalType.Float,
          c -> {
            c.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
          });
      builder.withCoercionConfig(
          LogicalType.Boolean,
          c -> {
            c.setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            c.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
          });
    };
  }
}
