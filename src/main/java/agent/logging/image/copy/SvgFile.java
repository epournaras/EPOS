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
package agent.logging.image.copy;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 *
 * @author Peter
 */
public class SvgFile extends ImageFile {

    private SVGGraphics2D svgGraphics;
    private Dimension dim;

    public SvgFile(File file, int width, int height) {
        super(file);
        dim = new Dimension(width, height);
    }

    @Override
    public Graphics2D createGraphics() {
        DOMImplementation domImpl
                = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);

        svgGraphics = new SVGGraphics2D(document);
        svgGraphics.setSVGCanvasSize(dim);
        return svgGraphics;
    }

    @Override
    public void write() {
        boolean useCSS = true; // we want to use CSS style attributes
        //try (Writer out = new OutputStreamWriter(System.out, "UTF-8")) {
        try (Writer out = new FileWriter(getFile())) {
            svgGraphics.stream(out, useCSS);
        } catch (IOException ex) {
            Logger.getLogger(SvgFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
