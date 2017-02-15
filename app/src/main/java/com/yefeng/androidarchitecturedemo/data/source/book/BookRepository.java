package com.yefeng.androidarchitecturedemo.data.source.book;

import android.support.annotation.NonNull;

import com.yefeng.androidarchitecturedemo.data.model.book.Book;
import com.yefeng.androidarchitecturedemo.data.source.book.local.BookLocalDataSource;
import com.yefeng.androidarchitecturedemo.data.source.book.memory.BookMemoryDataSource;
import com.yefeng.androidarchitecturedemo.data.source.book.remote.BookRemoteDataSource;

import java.util.List;

import io.reactivex.Flowable;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

/**
 * Created by yefeng on 19/01/2017.
 * book repository
 */

public class BookRepository implements BookDataSource {

    private static BookRepository INSTANCE = null;

    @NonNull
    private final BookRemoteDataSource mBookRemoteDataSource;

    @NonNull
    private final BookLocalDataSource mBookLocalDataSource;

    @NonNull
    private final BookMemoryDataSource mBookMemoryDataSource;

    private BookRepository(@NonNull BookRemoteDataSource bookRemoteDataSource,
                           @NonNull BookLocalDataSource bookLocalDataSource,
                           @NonNull BookMemoryDataSource bookMemoryDataSource) {
        mBookLocalDataSource = checkNotNull(bookLocalDataSource);
        mBookRemoteDataSource = checkNotNull(bookRemoteDataSource);
        mBookMemoryDataSource = checkNotNull(bookMemoryDataSource);
    }

    public static BookRepository getInstance(@NonNull BookRemoteDataSource bookRemoteDataSource,
                                             @NonNull BookLocalDataSource bookLocalDataSource,
                                             @NonNull BookMemoryDataSource bookMemoryDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new BookRepository(bookRemoteDataSource, bookLocalDataSource, bookMemoryDataSource);
        }
        return INSTANCE;
    }


    /**
     * Gets books from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     */
    @Override
    public Flowable<List<Book>> getBooks() {
        return Flowable.concat(mBookMemoryDataSource.getBooks(), getAndCacheLocalBooks(), getAndSaveRemoteBooks());
    }

    public Flowable<List<Book>> getBooks(boolean forceUpdate) {
        if (forceUpdate) {
            return getBooks();
        }
        return getBooks()
                .filter(books -> !books.isEmpty())
                .take(1);
    }

    private Flowable<List<Book>> getAndCacheLocalBooks() {
        return mBookLocalDataSource.getBooks().doOnNext(books -> {
            mBookMemoryDataSource.clear();
            mBookMemoryDataSource.saveBooks(books);
        });
    }

    private Flowable<List<Book>> getAndSaveRemoteBooks() {
        return mBookRemoteDataSource.getBooks()
                .doOnNext(books -> {
                    mBookMemoryDataSource.clear();
                    mBookLocalDataSource.deleteBooks();
                    mBookMemoryDataSource.saveBooks(books);
                    mBookLocalDataSource.saveBooks(books);
                });
    }


    @Override
    public void saveBook(@NonNull Book book) {

    }

    public Flowable<String> saveBookRx(@NonNull Book book) {
        return Flowable.fromPublisher(mBookRemoteDataSource.saveBookRx(book))
                .doOnNext(s -> {
                    mBookMemoryDataSource.saveBook(book);
                    mBookLocalDataSource.saveBook(book);
                });
    }

    @Override
    public void deleteBook(@NonNull String sampleBookId) {
    }
}
