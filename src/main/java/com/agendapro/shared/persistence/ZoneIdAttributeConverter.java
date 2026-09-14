package com.agendapro.shared.persistence;

import java.time.ZoneId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZoneIdAttributeConverter
		implements AttributeConverter<ZoneId, String> {

	@Override
	public String convertToDatabaseColumn(ZoneId zoneId) {
		return zoneId == null ? null : zoneId.getId();
	}

	@Override
	public ZoneId convertToEntityAttribute(String valor) {
		return valor == null ? null : ZoneId.of(valor);
	}
}
