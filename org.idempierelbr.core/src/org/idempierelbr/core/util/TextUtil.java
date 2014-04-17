package org.idempierelbr.core.util;

import java.util.Arrays;

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
	
	/**
	 * 	Verifica se lista cont�m uma determinada string.
	 * 
	 * 	@param stra
	 * 	@param strings
	 * 	@return TRUE se a lista cont�m a string, sen�o FALSO
	 */
	public static boolean match (Object obj, Object... objects)
	{
		if (obj == null || objects == null || objects.length == 0)
			return false;

		return Arrays.asList (objects).contains (obj);
	}
}
