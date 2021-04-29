/*
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.octyl.rss.dokusha.rss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.octyl.rss.dokusha.config.Configuration;
import net.octyl.rss.dokusha.config.RssSource;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class RssSeenEntries implements AutoCloseable {

    public static RssSeenEntries open(Path dataFile) throws IOException {
        Files.createDirectories(dataFile.getParent());
        var entriesDb = DBMaker.fileDB(dataFile.toFile())
            .fileMmapEnableIfSupported()
            .transactionEnable()
            .fileChannelEnable()
            .make();
        var seenEntries = entriesDb.hashSet("seen")
            .serializer(Serializer.STRING)
            .createOrOpen();
        return new RssSeenEntries(entriesDb, seenEntries);
    }

    private final DB db;
    private final HTreeMap.KeySet<String> seenEntries;

    private RssSeenEntries(DB db, HTreeMap.KeySet<String> seenEntries) {
        this.db = db;
        this.seenEntries = seenEntries;
    }

    /**
     * If {@code id} hasn't been seen, add it and return {@code true}.
     *
     * @param id the id
     * @return {@code true} if the id is new
     */
    public boolean addIfUnseen(String id) {
        var result = seenEntries.add(id);
        // if we made changes, commit them
        if (result) {
            db.commit();
        }
        return result;
    }

    @Override
    public void close() {
        db.close();
    }
}
