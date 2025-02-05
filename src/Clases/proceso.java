/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Clases;

/**
 *
 * @author andre
 */
public class proceso {
    private int id;
    private String estado;
    private String nombre;
    private String pc;
    private String mar;
    private int instrucciones;
    
    
    public proceso(int id, String nombre, int instrucciones) {
        this.estado = "Listo";
        this.id = id;
        this.instrucciones = instrucciones;
        this.nombre = nombre;
        this.pc = "Listo";
        this.mar = "Listo";
    }
    
    public int getId(){
        return id;
    }
    
    public int getInstrucciones(){
        return instrucciones;
    }
    
    public String getNombre(){
        return nombre;
    }
    
    public String getEstado(){
        return estado;
    }
    
    public String getPC(){
        return pc;
    }
    
    public String getMar(){
        return mar;
    }
    
    public void setEstado(String estado){
        this.estado = estado;
    }
    
    public void setPC(String pc){
        this.estado = pc;
    }
    
    public void setMar(String mar){
        this.estado = mar;
    }
}
