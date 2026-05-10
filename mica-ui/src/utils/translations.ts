import type { AttributeDto, LocalizedStringDto } from 'src/models/Mica';

function flattenJSON(obj: Record<string, unknown>, prefix: string = ''): AttributeDto[] {
  return Object.entries(obj).reduce((acc: AttributeDto[], [key, value]) => {
    const pre = prefix.length ? prefix + '.' : '';
    if (typeof value === 'object' && value !== null) {
      return [...acc, ...flattenJSON(value as Record<string, unknown>, pre + key)];
    } else {
      return [...acc, { name: pre + key, value: String(value) }];
    }
  }, []);
}

function unflattenJSON(attributes: AttributeDto[]): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const attr of attributes) {
    const keys = attr.name.split('.');
    keys.reduce((acc: Record<string, unknown>, k: string, i: number) => {
      if (i === keys.length - 1) {
        acc[k] = attr.value;
      } else {
        acc[k] = acc[k] || {};
      }
      return acc[k] as Record<string, unknown>;
    }, result);
  }
  return result;
}

export function translationAsMap(translations: LocalizedStringDto[]): Record<string, AttributeDto[]> {
  return translations.reduce((acc: Record<string, AttributeDto[]>, { lang, value }) => {
    acc[lang] = flattenJSON(JSON.parse(value || '{}'), '');
    return acc;
  }, {});
}

export function mapAsTranslation(map: Record<string, AttributeDto[]>): LocalizedStringDto[] {
  return Object.entries(map).map(([lang, attrs]) => {
    return { lang, value: JSON.stringify(unflattenJSON(attrs)) };
  });
}
