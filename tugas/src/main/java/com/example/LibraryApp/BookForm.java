package com.example.LibraryApp;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BookForm extends JFrame {
    private void loadDataFromAPI(){
        try {
            URL url = new URL("http://localhost:4567/api/books");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String json = in.lines().collect(Collectors.joining());
            in.close();

            List<Book> books = new Gson().fromJson(json, new TypeToken<List<Book>>() {}.getType());
            
            //Kosongkan tabel
            tableModel.setRowCount(0);

            //Tambahkan data baru
            for (Book book : books) {
                Object[] row = {book.getId(), book.getTitle(), book.getAuthor()};
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil data:\n" + e.getMessage());
        }
    }

    private void addBookViaAPI() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Judul dan Penulis harus diisi!");
            return;
        }

        try {
            URL url = new URL("http://localhost:4567/api/books");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String jsonBody = new Gson().toJson(new Book(0, title, author));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                JOptionPane.showMessageDialog(this, "Buku berhasil ditambahkan!");
                titleField.setText("");
                authorField.setText("");
                loadDataFromAPI();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menambahkan buku. Code: " + responseCode);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error:\n" + e.getMessage());
        }
    }

    private void editBookViaAPI() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih buku yang ingin diedit.");
            return;
        }
    
        long id = Long.parseLong(tableModel.getValueAt(selectedRow,0).toString());
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
    
        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Judul dan Penulis harus diisi!");
            return;
        }
    
        try {
            URL url = new URL("http://localhost:4567/api/books/" + id);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
    
            String jsonBody = new Gson().toJson(new Book(id, title, author));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
    
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JOptionPane.showMessageDialog(this, "Buku berhasil diperbarui.");
                loadDataFromAPI();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal memperbarui buku. Code: " + responseCode);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error:\n" + e.getMessage());
        }
    }    
    
    private void deleteBookViaAPI() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih buku yang ingin dihapus.");
            return;
        }
    
        long id = Long.parseLong(tableModel.getValueAt(selectedRow,0).toString());
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus buku ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                URL url = new URL("http://localhost:4567/api/books/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                
                int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                    JOptionPane.showMessageDialog(this, "Buku berhasil dihapus.");
                    loadDataFromAPI();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menghapus buku. Code: " + responseCode);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error:\n" + e.getMessage());
            }
        }
    }    


    private JTable table;
    private DefaultTableModel tableModel;
    private final String[] columnNames = {"ID", "Title", "Author"};
    private JTextField titleField;
    private JTextField authorField;

    public BookForm() {
        setTitle("Book Manager GUI");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));

        JPanel inputPanel = new JPanel(new FlowLayout());
        titleField = new JTextField(15);
        authorField = new JTextField(15);
        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Author"));
        inputPanel.add(authorField);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Book");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> addBookViaAPI());
        editButton.addActionListener(e -> editBookViaAPI());
        deleteButton.addActionListener(e -> deleteBookViaAPI());
        refreshButton.addActionListener(e -> loadDataFromAPI());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        controlPanel.add(inputPanel);
        controlPanel.add(buttonPanel);

        add(controlPanel, BorderLayout.SOUTH);

        loadDataFromAPI();
    }// tutup constructor()

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BookForm gui;
            try {
                gui = new BookForm();
                gui.setVisible(true);
            } catch (Exception e) {
                Logger.getLogger(BookForm.class.getName()).log(Level.SEVERE, null, e);
            }
        });
    }// tutup public static void main
}// tutup class BookForm
