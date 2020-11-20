/*
 * Copyright (C) 2016 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Peter, Thomas Asikis
 */
public class Helper {
    public static void clearDirectory(File dir) {
        if(dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) { //some JVMs return null for empty dirs
                for (File f : files) {
                    if (f.isDirectory()) {
                        clearDirectory(f);
                    }
                    f.delete();
                }
            }
        }
    }
    
	/**
	 * Reads a file in the given path
	 * @param path the path in string format
	 * @return a stream of string containing each line per element
	 */
	public static Stream<String> readFile(String path) {
		try {
			return Files.lines(Paths.get(path), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
			throw new IllegalStateException("Path not found or not a file.");
		}
	}
	
	/**
	 * Creates a stream of string paths of all the files in directory tree under the provided path
	 * @param path, that points to a folder
	 * @return the stream of paths related to the files
	 */
	public static Stream<String> walkPaths(String path) {
		try {
			return Files.walk(Paths.get(path)).filter(f -> Files.isRegularFile(f)).map(p -> p.toString());
		} catch (IOException ex) {
			Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, ex.toString(), ex);
			
			throw new IllegalStateException("Path not found or not a folder.");
		}
	}
	
	public static String clearNumericString(String s) {
		return s.replaceAll("\\p{C}", "");
	}

	public static int clearInt(String initial) {
		return Integer.parseInt(clearNumericString(initial));
	}
}
