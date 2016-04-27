/*
 The MIT License (MIT)

Copyright (c) 2016 

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package reading.bada;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Biljana
 */
public class ReadingBada {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		File folder = new File("D:\\Documents\\bada_36");
		File outputFile = new File("D:\\Documents\\bada_36_txt\\bada_36.txt");
		File aircraftTypesFile = new File("D:\\Documents\\bada_36\\SYNONYM.LST");
		HashMap<String, String> synonymsMap = new HashMap<>();

		try {
			synonymsMap = readSynonyms(aircraftTypesFile);
			iterateFiles(folder, synonymsMap, outputFile);

		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + aircraftTypesFile + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + aircraftTypesFile + "'");
		}

	}

	private static void iterateFiles(File folder, HashMap<String, String> synonymsMap, File outputFile) {
		File[] folderFiles = folder.listFiles();
		List<File> files = new ArrayList<>();

		for (int i = 0; i < folderFiles.length; i++) {
			File file = folderFiles[i];
			if (file.isFile() && file.getName().endsWith(".PTF")) {
				files.add(folderFiles[i]);
			}
		}

		for (File file : files) {
			readFiles(file, synonymsMap, outputFile);
		}
	}

	private static void readFiles(File file, HashMap<String, String> synonymsMap, File outputFile) {
		List<String> opfDataList;
		List<String> splittedLines = new ArrayList();

		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			File opfFile = renameFileExtension(file.getPath(), "OPF");

			opfDataList = OpfReader.readFile(opfFile.getPath());
			splittedLines = convertFileToList(file, synonymsMap, opfDataList);
			splittedLines.add("");

			writeListToFile(splittedLines, outputFile);

			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + file + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + file + "'");
		}
	}

	private static HashMap<String, String> readSynonyms(File aircraftTypesFile) throws IOException {
		FileReader aircraftTypesFileReader = new FileReader(aircraftTypesFile);
		BufferedReader aircraftBufferedReader = new BufferedReader(aircraftTypesFileReader);

		HashMap<String, String> synonymsMap = new HashMap<>();
		List<String> aircraftTypesList = new ArrayList<>();
		String[] aircraftTypes = null;

		String line = "";
		String previousLine = "";

		while ((line = aircraftBufferedReader.readLine()) != null) {
			line = line.trim();

			if (!line.equals("") && !line.startsWith("CC") && !line.startsWith("CD")) {
				if (line.startsWith("-")) {
					aircraftTypesList.add(previousLine);
					previousLine = line;
				} else {
					previousLine += "   " + line;
				}
			}
		}

		aircraftTypesList.add(previousLine);
		//the first line is blank so let's get rid of it
		aircraftTypesList.remove(0);

		for (String listItem : aircraftTypesList) {
			aircraftTypes = listItem.split("__*");

			synonymsMap.put(aircraftTypes[0].trim().replace("- ", ""), aircraftTypes[2].trim().replaceAll("  *", ","));
		}

		aircraftBufferedReader.close();

		return synonymsMap;
	}

	private static List<String> convertFileToList(File file, HashMap<String, String> synonymsMap, List<String> opfDataList) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		List<String> fileToList = new ArrayList();
		String line;
		String[] lines, types;
		int index = 1;

		while ((line = bufferedReader.readLine()) != null) {
			String edditedLine = "";
			line = line.trim();

			if (line.startsWith("AC/Type")) {
				edditedLine += line.replace("AC/Type", "ICAO").replaceAll("_", "").replace(" ", "");
				types = edditedLine.split(":");

				if (synonymsMap.containsKey(types[index])) {
					edditedLine = edditedLine.replace(types[index], synonymsMap.get(types[index]));
				}
				fileToList.add(edditedLine.trim());

				for (String opf : opfDataList) {
					fileToList.add(opf);
				}
			} else {

				if (!line.equals("") && Character.isDigit(line.charAt(0))) {
					lines = line.split("\\|");

					for (String temp : lines) {
						edditedLine += temp.trim().replaceAll("  *", ";") + "\t";
					}

					if (edditedLine.trim().length() > 3) {
						fileToList.add(edditedLine.trim());
					}
				}
			}
		}

		return fileToList;
	}

	private static void writeListToFile(List<String> splittedLines, File fileName) throws IOException {

		FileWriter writer = new FileWriter(fileName, true);
		File appendedFile = fileName;

		if (!appendedFile.exists()) {
			appendedFile.createNewFile();
		}
		for (String str : splittedLines) {
			writer.write(str + "\n");
		}
		writer.close();
	}

	public static File renameFileExtension(String source, String newExtension) {
		String target;
		String currentExtension = getFileExtension(source);

		if (currentExtension.equals("")) {
			target = source + "." + newExtension;
		} else {
			target = source.replaceFirst(Pattern.quote("."
					+ currentExtension) + "$", Matcher.quoteReplacement("." + newExtension));

		}

		return new File(target);
	}

	public static String getFileExtension(String source) {
		int i = source.lastIndexOf('.');
		if (i > 0 && i < source.length() - 1) {
			source = source.substring(i + 1);
		}

		//source is returned as D\\Documents\\bada_36\\A306__
		return source;
	}

}
