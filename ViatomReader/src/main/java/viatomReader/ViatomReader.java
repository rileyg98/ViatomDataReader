package viatomReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import com.opencsv.CSVWriter;

public class ViatomReader {
	
	// Code translated from OSCAR
	static int header_size = 40;
	static int record_size = 5;
	static long timestampms;
	static int headersz;
	static int duration;
	static long datasize;
	static long records;
	static long resolution;

	public static void main(String[] args) {
		
		// Grab the file in name, this is arg 1, and file out is arg 2
		if(args.length < 2) {
			System.out.println("Please enter input and output file names to process.");
			System.exit(1);
		}
		String filein = args[0];
		String fileout = args[1];
		
		try (
				InputStream input = new FileInputStream(filein);
			)
		{
			// Read the file into a byte array
			long size = new File(filein).length();
			byte[] fileraw = new byte[(int) size];
			input.read(fileraw);
			input.close();
			// Have all the bytes now
			// Check header
			byte[] hdr = new byte[header_size];
			System.arraycopy(fileraw, 0, hdr, 0, header_size);
			if(!checkHeader(hdr, size)) {
				// Header bad
				System.out.println("Header incorrect. Exiting.");
				System.exit(1);
			}
			// Copy the raw data away from header
			byte[] actualdata = new byte[(int) (size-header_size)];
			System.arraycopy(fileraw, header_size, actualdata, 0, (int) (size-header_size));
			
			// Start reading the actual data
			// Basically go through the byte array and create rows for the extracted data
			List<String[]> csvData = new ArrayList<>();
			
			for(int i = 0; i < records; i++) {
				int startval = i*record_size;
				String[] csvRow = new String[4];
				// Create the timestamp as a string
				csvRow[0] = String.valueOf(timestampms+(resolution*i));
				// The first byte we should be reading is the SpO2
				csvRow[1] = String.valueOf(actualdata[startval]);
				csvRow[2] = String.valueOf(actualdata[startval+1]);
				if(actualdata[startval+2] == 0xFF) {
					// Oximetry invalid flag, means we need to mark this row as invalid
					csvRow[3] = String.valueOf(0);
				} else {
					csvRow[3] = String.valueOf(1);
				}
				csvData.add(csvRow);
			}
			// Finished collecting data
			// Now to confirm the validity of the resolution
			// 2000ms resolution is just 4000ms but two copies... we remove the duplicate if needed
			if(resolution == 2000) {
				boolean dup = true;
				List<String[]> csvDedup = new ArrayList<>();
				for(int i = 0; i < csvData.size(); i += 2) {
					String[] reca = csvData.get(i);
					String[] recb = csvData.get(i+1);
					if(!reca[1].equals(recb[1]) ||
							!reca[2].equals(recb[2]) ||
							!reca[3].equals(recb[3])) {
						// Not actually duplicated
						dup = false;
						break;
					}
					csvDedup.add(reca);
				}
				if(dup) {
					csvData = csvDedup;
				}
			}
			
			// Add the header
			String[] hdrCSV = {"Timestamp", "SpO2", "Heartrate", "Valid"};
			csvData.add(0, hdrCSV);
			// Our data is complete and needs to be written
			CSVWriter writer = new CSVWriter(new FileWriter(fileout));
			writer.writeAll(csvData);
			writer.close();
			System.out.println("Converted data to CSV. Exiting.");
			System.exit(0);
			
			
		} catch (IOException e) {
			System.out.println("Failed to read file. Exiting.");
			System.exit(1);
		}
		
		
		
	}
	private static boolean checkHeader(byte[] header, long filesz) {
		// Next step is to parse the header
		if(header.length < header_size) {
			// Not Viatom, too short
			System.out.println("Incorrect data format. This is not a Viatom file. Exiting.");
			System.exit(1);
		}
		// This part processes the time and signature
		int sig = header[0] | (header[1] << 8);
		byte[] yrbt = {header[2], header[3]};
		int year = bytesToShort(yrbt);
		int month = header[4];
		int day = header[5];
		int hour = header[6];
		int min = header[7];
		int sec = header[8];
		// Check signature
		if (sig != 0x0003) {
			System.out.println("Invalid signature for Viatom. Exiting.");
			return false;
		}
		// Check timestamp
		System.out.println("Year: " + year);
		System.out.println("Month: " + month);
		System.out.println("Day: " + day);
		System.out.println("Hour: " + hour);
		System.out.println("Minute: " + min);
		System.out.println("Second: " + sec);
		if ((year < 2015 || year > 2059) || (month < 1 || month > 12) || (day < 1 || day > 31) || (hour > 23) || (min > 59) || (sec > 59)) {
			System.out.println("Invalid timestamp. Exiting.");
			return false;
		}
		// Use a calendar to convert to milliseconds
		GregorianCalendar timestamp = new GregorianCalendar(year, month, day, hour, min, sec);
		timestampms = timestamp.getTimeInMillis();
		headersz = header[9] | (header[10] << 8);
		duration = header[13] | (header[14] << 8);
		// Do some timing resolution things
		datasize = filesz - header_size;
		records = datasize / record_size;
		resolution = duration / records*1000;
		return true;
		
	}
	public static short bytesToShort(byte[] bytes) {
	     return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
	}


}
