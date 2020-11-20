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
import java.io.File;

/**
 *
 * @author Peter
 */
public abstract class ImageFile {
    private final File file;
    
    public ImageFile(File file) {
        this.file = file;
    }
    
    public File getFile() {
        return file;
    }
    
    public abstract Graphics2D createGraphics();
    public abstract void write();
}
