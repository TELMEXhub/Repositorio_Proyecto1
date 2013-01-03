#include "C:\Android\Thub\Proyecto1\Proyecto1.h"
#include <string.h>
#byte TRISA = 85  // dirección del registro trisA
#byte puerto_a = 05   // dirección del puerto A
#bit RA0 = puerto_a.0 


#int_RDA
void  RDA_isr(void) {

   char entrada[5]="\0";
   fgets(entrada,BLUE); //GUARDAMOS EN ENTRADA LO QUE HAY EN LA USART

   char led1[3]="\0"; 
   char led2[3]="\0"; 

   led1="1";
   led2="2";
 //Y HACEMOS LAS COMPARACIONES DE CADENAS DE TEXTO.
 
   if(strcmp(entrada,led1)==0)RA0=0; //ON
   else if(strcmp(entrada,led2)==0)RA0=1;//OFF
  else {                     int i;    for( i=1; i<=10; i++){ 
 puerto_a=0x00;delay_us(300);puerto_a=0x03;delay_us(700); 
 puerto_a=0x00;delay_ms(50); puerto_a=0x03;delay_ms(50);}   }
 
}//fin de metodo interrupcion rda


void main() {  
   setup_comparator(NC_NC_NC_NC);//DESACTIVAMOS COMPARADORES
   setup_vref(FALSE);
   enable_interrupts(INT_RDA); //POR PUERTO DE COMUNICACIONES 
   enable_interrupts(GLOBAL);  //E INTERRUPCIONES GLOBALES

   set_tris_a( 0xfe); //puerto A como  1111 1110utput
   puerto_a =0x01;//limpiamos puerto, encendemos led con cero

   putc('Z'); //usaos por primera vez el puerto de comunicaciones

   while(true){     //Ciclo infinito en espera de interrupciones 

              }                                
            }//FIN D
