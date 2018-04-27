package com.example.srv.audiomain;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FileLoad {
    private Context context;
    private ArrayList<Music> musicList;

    public FileLoad(Context context){

        this.context = context;
        musicList = getMusicInfo();
    }

    public ArrayList<Music> getMusicList() {
        return musicList;
    }

    public ArrayList getMusicInfo(){

        List<Music> resultList = new ArrayList<Music>();
        String[] projection = {MediaStore.Audio.Media._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);

        while(cursor.moveToNext()){
            Music music = new Music();
            music.setId(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            music.setAlbumId(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
            music.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            music.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            music.setFilePath(cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));

            Log.d("FILE_PATH", cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
            resultList.add(music);
        }
        cursor.close();

        return (ArrayList) resultList;
    }

    public Uri getAlbumArt(long albumId){
        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(artworkUri, albumId);
        return uri;
    }

}
