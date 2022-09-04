package leiliang91;

import lombok.Data;

import java.util.List;

@Data
public class Trash {
    private String id;
    private String name;
    private String avatar;
    private List<String> reasons;
    private String profileUrl;
}
