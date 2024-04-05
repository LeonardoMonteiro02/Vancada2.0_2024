/**
 * Classe que representa uma região com suas informações básicas.
 *
 * Esta classe armazena o nome, latitude, longitude, timestamp e usuário associado a uma região específica.
 * Oferece métodos para acessar e modificar essas informações, além de implementar funcionalidades para comparação de objetos.
 *
 * Principais funcionalidades:
 * - Armazenamento e recuperação do nome, latitude, longitude, timestamp e usuário associado a uma região.
 * - Implementação de métodos para obter e definir essas informações.
 * - Implementação de métodos equals() e hashCode() para comparar objetos Region.
 *
 * Autor: Leonardo Monteiro
 * Data: 05/04/2024
 */


package com.example.avancada20.ui.home;

import java.util.Objects;

public class Region {
    private String name;
    private double latitude;
    private double longitude;
    private Long timestamp;
    private int user;

    public Region(String name, double latitude, double longitude,Long timestamp, int user) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.user = user;
    }
    public Region(){}

    public String getName() {
        return name;
    }
    public Long getTimestamp(){return timestamp;}
    public int getuser(){return user;}
    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    /**
     * Verifica se este objeto Region é igual a outro objeto.
     * Retorna verdadeiro se os objetos forem iguais e falsos caso contrário.
     *
     * @param obj O objeto a ser comparado com este objeto Region.
     * @return True se os objetos forem iguais, False caso contrário.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Region other = (Region) obj;
        return Double.compare(other.latitude, latitude) == 0 && // Compara as latitudes
                Double.compare(other.longitude, longitude) == 0 && // Compara as longitudes
                Objects.equals(other.name, name) && // Compara os nomes
                Objects.equals(other.timestamp, timestamp) && // Compara os timestamps
                other.user == user; // Compara os usuários
    }



    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude);
    }

}
