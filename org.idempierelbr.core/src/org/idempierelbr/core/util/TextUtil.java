package org.idempierelbr.core.util;

public class TextUtil {
	/**
	 * Obt�m apenas os n�meros [0-9] de uma String
	 * @return String apenas os n�meros, null ou vazio
	 */
	public String getNumbersOnly(String text)
	{
		if (text == null)
			return null;

		return text.trim().replaceAll("[^0-9]*", "");
	}
}
