package leiliang91;

import lombok.Data;

import java.util.Map;

@Data
public class Trashes {
    private Map<String, Trash> trashesMap;
    private long lastUpdated;
}
