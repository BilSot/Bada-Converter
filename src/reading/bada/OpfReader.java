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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Biljana
 */
public class OpfReader {
	private static final DecimalFormat MACH_FORMAT = new DecimalFormat("0.0##");
	private static final DecimalFormat KNOTS_FORMAT = new DecimalFormat("0");

	static {
		MACH_FORMAT.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		KNOTS_FORMAT.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	}
	
	public static List<String> readFile(String fileName) {
		List<String> opfData = new ArrayList<>();
		File file = new File(fileName);
		HashMap<String, List<String>> sectionDataMap = readSectionMap(file);

		parseSpeeds(sectionDataMap, opfData);
		parseVortex(sectionDataMap, opfData);
		parseVstall(sectionDataMap, opfData);
		
//		System.out.println(KNOTS_FORMAT.format(vmo) + " " + MACH_FORMAT.format(mmo));
//		for (Map.Entry<String, List<String>> entrySet : sectionDataMap.entrySet()) {
//			System.out.println(entrySet.getKey());
//
//			for (String value : entrySet.getValue()) {
//				System.out.println("\t" + value);
//			}
//		}
//		for (String opfData1 : opfData) {
//			System.out.println(opfData1);
//		}
		
		return opfData;

	}

	private static void parseSpeeds(HashMap<String, List<String>> sectionDataMap, List<String> opfData){
		String line = sectionDataMap.get("Flight envelope").get(1).trim();
		String[] speeds = line.split("  *");
		float vmo = Float.parseFloat(speeds[0]);
		float mmo = Float.parseFloat(speeds[1]);
		
		opfData.add("VMO:" + KNOTS_FORMAT.format(vmo));
		opfData.add("MMO:" + MACH_FORMAT.format(mmo));
	}

	private static void parseVortex(HashMap<String, List<String>> sectionDataMap, List<String> opfData) {
		String line = sectionDataMap.get("Actype").get(0).trim();
		String[] vortex = line.split("  *");
		
		opfData.add("VORTEX:" + vortex[vortex.length - 1]);
	}

	private static void parseVstall(HashMap<String, List<String>> sectionDataMap, List<String> opfData) {
		String line = sectionDataMap.get("Aerodynamics").get(5).trim();
		String[] vstall = line.split("  *");
		float vstallValue = Float.parseFloat(vstall[3]);
		
		opfData.add("Vstall:" + KNOTS_FORMAT.format(vstallValue));
	}

	private static HashMap<String, List<String>> readSectionMap(File file) {
		HashMap<String, List<String>> sectionDataMap = new HashMap<>();
		List<String> opfLines = readOpfFile(file);

		List<String> tempValues = null;
		for (String opfLine : opfLines) {
			if (opfLine.startsWith("=")) {
				tempValues = new ArrayList<>();
				String key = opfLine.replaceAll("==*", "").trim();
				sectionDataMap.put(key, tempValues);
			} else {
				tempValues.add(opfLine);
			}
		}
		return sectionDataMap;
	}

	private static List<String> readOpfFile(File file) {
		List<String> opfData = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "";
			//skipping header lines
			while (!(line = reader.readLine()).startsWith("CD"));

			while ((line = reader.readLine()) != null) {
				opfData.add(line.replace("CC", "").replace("CD", "").replace("/", "").trim());
			}

		} catch (FileNotFoundException ex) {
			Logger.getLogger(OpfReader.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(OpfReader.class.getName()).log(Level.SEVERE, null, ex);
		}
		return opfData;
	}

//	public static void main(String[] args) {
//		OpfReader.readFile("D:\\Documents\\bada_36\\A306__.OPF");
//	}
}
