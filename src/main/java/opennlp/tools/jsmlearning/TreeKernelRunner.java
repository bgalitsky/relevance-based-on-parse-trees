package opennlp.tools.jsmlearning;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TreeKernelRunner {
	private void runEXE(String[] command, String runPath){
		Runtime r = Runtime.getRuntime();
		Process mStartProcess = null;
		try {
			mStartProcess = r.exec( command, null, new File(runPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StreamLogger outputGobbler = new StreamLogger(mStartProcess.getInputStream());
		outputGobbler.start();

		try {
			int returnCode = mStartProcess.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void runLearner(String dir, String learning_file, String  model_file)
	{
		dir = dir.replace('/', '\\');
		
		if (!dir.endsWith("\\"))
				dir+="\\";
		String[] runString = new String[]{dir+"svm_learn.exe","-t", "5", dir+learning_file,  dir+model_file};
		runEXE(runString, dir);
	}
	
	
	//svm_classify example_file model_file predictions_file
	public void runClassifier(String dir, String example_file, String  model_file, String predictions_file)
	{
		dir = dir.replace('/', '\\');
		
		if (!dir.endsWith("\\"))
				dir+="\\";
		String[] runString = new String[]{dir+"svm_classify.exe", dir+example_file,  dir+model_file, dir+predictions_file};
		runEXE(runString, dir);
	}

	class StreamLogger extends Thread{

		private InputStream mInputStream;

		public StreamLogger(InputStream is) {
			this.mInputStream = is;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(mInputStream);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}
	
	public static void main(String[] args){
		TreeKernelRunner runner = new TreeKernelRunner();
		runner.runLearner("C:\\stanford-corenlp\\tree_kernel\\", "training.txt", "arg0.model1.txt");
		runner.runClassifier("C:\\stanford-corenlp\\tree_kernel\\", "arg0.test", "arg0.model1.txt", "arg0.output1.txt");
	}
}

	/*
exec:

public Process exec(String command, String envp[], File dir) 



   @param      command   a specified system command.
   @param      envp      array of strings, each element of which 
                         has environment variable settings in format
                         <i>name</i>=<i>value</i>.
   @param      dir       the working directory of the subprocess, or
                         <tt>null</tt> if the subprocess should inherit
                         the working directory of the current process.

                         В дистрибутиве два exe-файла: svm_learn.exe и svm_classify.exe.

1.   svm_learn.exe берет файл с примерами, обрабатывает его, строит файл model м правилами обучение.

Примеры запуска: 
svm_learn -t 5 learning_file model_file - это самый простой вариант запуска, SubSetTreeKernel (допускаются разрывы при обходе деревьев)

svm_learn -t 5 -D 0 learning_file model_file - другой вариант ядра, SubTreeKernel

Пример файла лежит на его страничке. Там же описание параметров.

2. svm_classify.exe берет файл с тестовыми примерами, файл с моделью, построенный svm_learn, и записывает результаты обучения в файл predictions_file.

Запуск:     svm_classify example_file model_file predictions_file

Файл имеет тот же формат, что и входные примеры. Образец лежит в архиве на страничке Москитти. 
Можно сразу же указывать, к какому классу относится пример (1 или -1 в начале строки). В этом случае точность и полнота оцениваются автоматически. Или ставить там 0.
	 */