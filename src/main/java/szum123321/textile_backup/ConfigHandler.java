/*
 * Simple backup mod made for Fabric and ported to Forge
 *     Copyright (C) 2020  Szum123321
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package szum123321.textile_backup;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigHandler {
    /*
        This part here is based on the cotton config by The Cotton Project authors.
        License requires me to provide you a copy of their license, so here it goes.

                MIT License

        Copyright (c) 2018 The Cotton Project

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

    private static File getConfigFile() {
        return FMLLoader
                .getGamePath()
                .resolve("config/")
                .resolve(TextileBackup.MOD_ID + ".json5")
                .toFile();
    }

    public static ConfigData loadConfig(){
        File configFile = getConfigFile();

        if(!configFile.exists()) {
            TextileBackup.logger.info("Creating new config file");
            saveConfig(new ConfigData());
        }

        try {
            Jankson jankson = Jankson.builder().build();
            return jankson.fromJson(jankson.load(configFile), ConfigData.class);
        } catch (IOException | SyntaxError e) {
            TextileBackup.logger.error(e.getMessage());
        }

        TextileBackup.logger.info("Loading default config!");
        return new ConfigData();
    }

    public static void saveConfig(ConfigData configData) {
        File configFile = getConfigFile();
        Jankson jankson = Jankson.builder().build();

        try{
            if(!configFile.exists()){
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }

            FileOutputStream stream = new FileOutputStream(configFile, false);
            stream.write(jankson.toJson(configData).toJson(true, true).getBytes());
            stream.flush();
            stream.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
