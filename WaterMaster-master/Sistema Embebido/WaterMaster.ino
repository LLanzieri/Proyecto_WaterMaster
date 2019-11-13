/*-----------------------------*/
// DECLARACION DE VARIABLES PARA PINES
/*-----------------------------*/

/*-----------------------------*/
//PINES DE SENSOR DE CARGA
/*-----------------------------*/
#include <HX711.h>
#define DOUT  A14
#define CLK  A13
HX711 balanza(DOUT, CLK);

/*-----------------------------*/
//LIBRERIA PARA EL BLUETOOTH
#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial  
SoftwareSerial BT(10, 11); // Definimos los pines RX y TX del Arduino conectados al Bluetooth
/*-----------------------------*/

/*-----------------------------*/
// Librerias del sensor de temperatura
/*-----------------------------*/

#include <OneWire.h>
#include <DallasTemperature.h>
OneWire ourWire(52);                //Se establece el pin 52 como bus OneWire
DallasTemperature sensors(&ourWire); //Se declara una variable u objeto para nuestro sensor

/*-----------------------------*/
// Declaracion variables del ultrasonido
/*-----------------------------*/

const int pintrigger = 24;
const int pinecho = 22;

/*-----------------------------*/
// pin de la bocina
/*-----------------------------*/

const int pinbuzzer = 2;

/*-----------------------------*/
//pin del sensor de agua
/*-----------------------------*/

const int pinsensoragua = A15;

/*-----------------------------*/
// PINES DE LOS LEDS
/*-----------------------------*/

const int ledRojo = 30;
const int ledAzul = 34;
const int ledVerde = 37;
const int ledResistencia = 8;
const int ledAmarillo = 42;

/*-----------------------------*/
//PIN RELE
/*-----------------------------*/

const int pinrele = 26;

/*-----------------------------*/
// VARIABLES PARA CALCULOS
/*-----------------------------*/

unsigned int tiempo, distancia;
const int nivelmaximoagua = 100;
const int distanciamaxima = 11;
const float pesoactivacion = 15;
int nivelagua = 0;

String estadoAnterior;
String inicial = "-" ;
String mensaje = inicial;

/*-----------------------------*/
//"112": mando una p - desactivar bomba
//"115": mando una s - apagar bocina
//"100" : mando una d - mayor brillo en el led
//"105" : mando una i - menor intensidad en el led
//"102" : mando una f - agua fria
//"98" : mando una b - entro al modo balanza
//"99" : mando una c - agua caliente
//"122" : mando una z - desconexion del bt
/*-----------------------------*/

int brillo = 0;
int temperaturaMaxima = 0;

void setup() {

  BT.begin(9600);

  // PREPARAR LA COMUNICACION SERIAL
  Serial.begin(9600);

  // CONFIGURAR PINES DE ENTRADA Y SALIDA
  pinMode(pinecho, INPUT);
  pinMode(pintrigger, OUTPUT);
  pinMode(pinbuzzer, OUTPUT);  //definir pin como salida
  pinMode(pinrele, OUTPUT); // DEFINO PIN DEL RELE como salida
  pinMode(ledRojo, OUTPUT);
  pinMode(ledAzul, OUTPUT);
  pinMode(ledVerde, OUTPUT);
  pinMode(ledResistencia, OUTPUT);
  pinMode(ledAmarillo, OUTPUT);

  //INICIO EL SENSOR DE TEMPERATURA
  sensors.begin();

  balanza.set_scale(1210);  //La escala por defecto es 1
  balanza.tare(20);         //El peso actual es considerado Tara.

}

void loop() {

  medirNivelaguarecipiente();       // mando el pulso del ultrasonido

  if (BT.available())
    mensaje = BT.read();

  Serial.println("Imprimiendo mensaje de android");
  Serial.println();
  Serial.println(mensaje);
  Serial.println("-----------------------------------------");

  if (mensaje == "122")     // mande una "z" para la desconexión
  {
    digitalWrite(ledAzul, LOW);
    digitalWrite(ledRojo, LOW);
    digitalWrite(ledAmarillo, LOW);
    digitalWrite(ledResistencia, LOW);
    mensaje = inicial;
  }

  if (mensaje == inicial)
    enviarTemperatura();

  if (mensaje == "115")       // si el mensaje es una "s"
    desactivarBuzzer();


  if (mensaje == "98")    // si me mandan una "b"
    modoBalanza();
  else
  {
    if (distancia > distanciamaxima && mensaje != "115")  // si la distancia del agua es mayor a la distancia maxima y msj distinto de "s"
      activarBuzzer();
    else
    { desactivarBuzzer();
    
      // podes comenzar a servir

      if (mensaje == "102")  // si el mensaje es una "f"
      { enviarTemperatura();
        desactivarBuzzer();
        digitalWrite(ledAzul, HIGH);
        digitalWrite(ledRojo, LOW);
        digitalWrite(ledResistencia, LOW);
      } else
      {
        if (mensaje == "99" || mensaje == "100" || mensaje == "105")   // si el mensaje es una "c"
        {
          controlartemperatura();
          desactivarBuzzer();
          brillo = 10;
          mensaje = "99";

          digitalWrite(ledAzul, LOW);
          digitalWrite(ledRojo, HIGH);
          digitalWrite(ledResistencia, LOW);

          while (!haypeso() && mensaje != "102" && mensaje != "98" && mensaje != "122") // si no hay peso y el mensaje es distinto de "f" quiere decir que sigue queriendo agua caliente
          {
            enviarTemperatura();

            if (BT.available())
              mensaje = BT.read();

            if (mensaje == "100")
              brillo += 30;
            if (mensaje == "105")
              brillo -= 30;

            if (brillo > 255)
              brillo = 255;
            if (brillo < 0)
              brillo = 10;

            analogWrite(ledResistencia, brillo);
            delay(4);

            Serial.print("mensaje: ");
            Serial.println(mensaje);
            Serial.print("brillo: ");
            Serial.println(brillo);

          }
        }
      }

      int vaso = haypeso();
      if (vaso == 1)
        activarbomba();
    }
  }
}

void activarBuzzer()
{
  Serial.println("ESTOY ACTIVANDO BUZZER");

  digitalWrite(pinbuzzer, LOW);   // poner el Pin en HIGH
  digitalWrite(pinrele, HIGH);
  digitalWrite(ledAzul, LOW);
  digitalWrite(ledRojo, LOW);
  enviarTemperatura();
}

void desactivarBuzzer()
{
  Serial.println("ESTOY DESACTIVANDO BUZZER");
  digitalWrite(pinbuzzer, HIGH);
  digitalWrite(pinrele, HIGH);

  if (distancia < distanciamaxima && mensaje == "115")
  { digitalWrite(ledAzul, HIGH);
    mensaje = "102";
  }

}

bool controlartemperatura()
{
  sensors.requestTemperatures();              //Se envía el comando para leer la temperatura
  float temp = sensors.getTempCByIndex(0);    //Se obtiene la temperatura en ºC

  Serial.print("Temperatura= ");
  Serial.print(temp);
  Serial.println(" C");

  if (temp > temperaturaMaxima)
    return false;
  else
    return true;
}

// funcion para sensar el peso
int haypeso()
{
  float peso = balanza.get_units(20);
  Serial.print("Sensor Peso = " );
  Serial.println(peso);

  if (peso > pesoactivacion)
    return 1;
  else
  {
    digitalWrite(ledVerde, LOW);
    return 0;
  }
}


//funcion de activar/cortar bomba por medio del rele
void activarbomba()
{
  while (sensarnivelagua() < nivelmaximoagua && haypeso() == 1 && mensaje != "112")
  {

    enviarTemperatura();
    digitalWrite(ledAmarillo, HIGH);

    Serial.println("ESTOY ACTIVANDO BOMBA");
    digitalWrite(pinrele, LOW);   // poner el rele en HIGH

    if (BT.available())
      mensaje = BT.read();

    Serial.println("--------------------------------");
    Serial.println(mensaje);

  }

  //RELE=0
  Serial.println("ESTOY DESACTIVANDO BOMBA");
  digitalWrite(ledAmarillo, LOW);
  digitalWrite(ledVerde, HIGH);
  enviarTemperatura();

}

// funcion para tomar el valor del sensor de nivel de agua
int sensarnivelagua()
{
  nivelagua = analogRead(pinsensoragua);

  Serial.print("Sensor de agua = " );
  Serial.println(nivelagua);

  return nivelagua;

}


int medirNivelaguarecipiente() {

  // ENVIAR PULSO DE DISPARO EN EL PIN "TRIGGER"
  digitalWrite(pintrigger, LOW);
  delayMicroseconds(2);
  digitalWrite(pintrigger, HIGH);
  // EL PULSO DURA AL MENOS 10 uS EN ESTADO ALTO
  delayMicroseconds(10);
  digitalWrite(pintrigger, LOW);
  // MEDIR EL TIEMPO EN ESTADO ALTO DEL PIN "ECHO" EL PULSO ES PROPORCIONAL A LA DISTANCIA MEDIDA
  tiempo = pulseIn(pinecho, HIGH);

  // LA VELOCIDAD DEL SONIDO ES DE 340 M/S O 29 MICROSEGUNDOS POR CENTIMETRO
  // DIVIDIMOS EL TIEMPO DEL PULSO ENTRE 58, TIEMPO QUE TARDA RECORRER IDA Y VUELTA UN CENTIMETRO LA ONDA SONORA
  distancia = tiempo / 58;

  // ENVIAR EL RESULTADO AL MONITOR SERIAL
  Serial.print(distancia);
  Serial.println(" cm");

}


bool enviarTemperatura()
{
  sensors.requestTemperatures();            //Se envía el comando para leer la temperatura
  float temp = sensors.getTempCByIndex(0);  //Se obtiene la temperatura en ºC

  Serial.print("Temperatura= ");
  Serial.print(temp);
  Serial.println(" C");

  BT.print(" ");
  BT.print(temp);
  BT.print("t");

}

int modoBalanza()
{
  float peso = balanza.get_units(20);
  Serial.print("Sensor Peso = " );
  Serial.println(peso);
  digitalWrite(ledResistencia, LOW);
  digitalWrite(ledAzul, LOW);
  digitalWrite(ledRojo, LOW);
  digitalWrite(ledAmarillo, HIGH);
  delay(500);
  digitalWrite(ledAmarillo, LOW);

  BT.print(" ");
  BT.print(peso);
  BT.print("p");

}
