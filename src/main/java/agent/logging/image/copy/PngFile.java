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

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Peter
 */
public class PngFile extends ImageFile {

    private final BufferedImage img;

    public PngFile(File file, int width, int height) {
        super(file);
        img = new BufferedImage(width, height, ColorSpace.TYPE_CMYK);
    }

    @Override
    public Graphics2D createGraphics() {
        return img.createGraphics();
    }

    @Override
    public void write() {
        try {
            ImageIO.write(img, "png", getFile());
        } catch (IOException ex) {
            Logger.getLogger(PngFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
