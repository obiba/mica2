import type { AttributeConfigurationDto, AttributeDto } from 'src/models/Mica';
import type { SchemaFormField, SchemaFormObject } from 'src/components/models';
import { t } from 'src/boot/i18n';

export function attributesToSchema(attributes: AttributeConfigurationDto[], title: string, description: string) {
  const schema = {
    $schema: 'http://json-schema.org/schema#',
    title: title || '',
    description: description || '',
    type: 'array',
    items: [] as SchemaFormField[],
    required: [],
  } as SchemaFormObject;

  (attributes || []).forEach((attribute: AttributeConfigurationDto) => {
    const type = attribute.type.toLowerCase();
    const field = {
      key: attribute.name,
      type: type,
      title: t(attribute.name) || attribute.name,
      description: attribute.description ? t(attribute.description) || attribute.description : undefined,
    } as SchemaFormField;

    switch (attribute.type.toLowerCase()) {
      case 'number':
      case 'integer':
      case 'boolean':
      case 'string':
        if (type === 'string' && attribute.values) {
          field.enum = attribute.values.map((value: string) => ({ key: value, title: value }));
          field.default = attribute.values[0] || '';
        }

        schema.items.push(field);
        if (attribute.required) {
          schema.required.push(attribute.name);
        }
        break;
    }
  });

  return schema;
}

export function splitAttributes(attributes: AttributeDto[], systemAttributes: AttributeConfigurationDto[]) {
  const systemAttributesMap = new Map<string, AttributeConfigurationDto>();
  systemAttributes.forEach((attribute: AttributeConfigurationDto) => {
    systemAttributesMap.set(attribute.name, attribute);
  });

  const custom = [] as AttributeDto[];
  const specific = [] as AttributeDto[];

  (attributes || []).forEach((attribute: AttributeDto) => {
    if (systemAttributesMap.has(attribute.name)) {
      custom.push(attribute);
    } else {
      specific.push(attribute);
    }
  });

  return { custom, specific };
}
