package Test;

public class Test {

	public static void main(String[] args) {
		// Exercise 5.6
		int i = 0;
		do {
			i++;
		} while (i < 10);

		// Exercise 5.7
		for (int j = 0; j < 5; j++) {
			System.out.println(j);
		}

		// Exercise 5.11
		boolean flag = False;
		flag = flag ? False : True;
		System.out.println(flag);

		// Exercise 5.12
		int successes = 0;
		if (1 < 3 || 3 < 1) {
			successes++;
		}

		if (2 < 1 || 1 > 2) {
			successes--;
		}

		if (2 < 1 || 5 > 2) {
			sucesses++;
		}

		// Exercise 5.21
		long bigNum = 1234567890987654321;
		System.out.println(bigNum);

	}

}

/*public class Toast {

}*/