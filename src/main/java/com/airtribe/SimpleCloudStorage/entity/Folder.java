package com.airtribe.SimpleCloudStorage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@AllArgsConstructor
public class Folder {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int folder_id;
    private int user_id;
    private String folderName;
    private Integer parent_id;
    private Date created_at;
    @OneToMany(mappedBy = "folder",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<File> files;

    public Folder(){

    }

}
