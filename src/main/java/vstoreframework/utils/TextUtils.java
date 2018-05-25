package vstoreframework.utils;

public class TextUtils {
	private TextUtils() {}
	
	//Taken from Android
	//Source: android.text.TextUtils
	public static String join(CharSequence delimiter, @SuppressWarnings("rawtypes") Iterable tokens) {
		StringBuilder sb = new StringBuilder();
		boolean firstTime = true;
		for (Object token: tokens) 
		{
			if (firstTime) 
			{
				firstTime = false;
			}
			else 
			{
				sb.append(delimiter);
			}
			sb.append(token);
		}
		return sb.toString();
	}
}
