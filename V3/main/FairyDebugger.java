package cn.anxinhainan.fairy;

public class FairyDebugger {
	private static boolean enabled = false;
	
	public static void debug(String str) {
		if (enabled) {
			System.out.println(str);
		}
 	}
}
