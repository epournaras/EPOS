/*
 * Copyright (C) 2016 peter
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
package data.io;

import data.Vector;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Provides functions to read vectors from various sources.
 *
 * @author peter
 */
public class VectorIO {

    /**
     * Reads a vector from the given file. The format is expected to be a comma
     * separated list of all values of the vector.
     *
     * @param vectorFile the file that contains the vector data
     * @return a new Vector object loaded from the file
     * @throws FileNotFoundException if the file could not be found
     */
    public static Vector readVector(File vectorFile) throws FileNotFoundException {
        return readVector(new Scanner(vectorFile));
    }

    /**
     * Parses the Vector contained in the given String. The format is expected
     * to be a comma separated list of all values of the vector.
     *
     * @param vectorStr the String that contains the vector data
     * @return a new Vector object parsed from the String
     */
    public static Vector parseVector(String vectorStr) {
        return readVector(new Scanner(vectorStr));
    }

    private static Vector readVector(Scanner scanner) {
        List<Double> values = new ArrayList<>();

        scanner.useLocale(Locale.US);

        scanner.useDelimiter(",");
        while (scanner.hasNextDouble()) {
            values.add(scanner.nextDouble());
        }

        Vector vector = new Vector(values.size());
        for (int i = 0; i < values.size(); i++) {
            vector.setValue(i, values.get(i));
        }

        return vector;
    }
}
