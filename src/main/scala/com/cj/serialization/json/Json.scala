package com.cj.serialization
package json

import traversals._

sealed abstract class Json extends Product with Serializable {

  import JsonS._

  def fold[X](
               withNull: => X,
               withBoolean: Boolean => X,
               withNumber: BigDecimal => X,
               withString: String => X,
               withArray: List[X] => X,
               withAssoc: Map[String, X] => X
             ): X = {

    val recurse: Json => X =
      _.fold(withNull,withBoolean,withNumber,withString,withArray,withAssoc)

    this match {
      case JNull => withNull
      case JBool(p) => withBoolean(p)
      case JNumber(x) => withNumber(x)
      case JString(x) => withString(x)
      case JArray(x) => withArray(x.map(recurse))
      case JAssoc(x) => withAssoc(x.mapValues(recurse))
    }
  }

  def nul: Option[Unit] = this match {
    case JNull => Some(())
    case _ => None
  }

  def bool: Option[Boolean] = this match {
    case JBool(p) => Some(p)
    case _ => None
  }

  def number: Option[BigDecimal] = this match {
    case JNumber(x) => Some(x)
    case _ => None
  }

  def long: Option[Long] = this match {
    case JNumber(x) => x.toLong match {
      case y if BigDecimal(y) == x => Some(y)
      case _ => None
    }
    case _ => None
  }

  def double: Option[Double] = this match {
    case JNumber(x) => x.toDouble match {
      case y if BigDecimal(y) == x => Some(y)
      case _ => None
    }
    case _ => None
  }

  def string: Option[String] = this match {
    case JString(x) => Some(x)
    case _ => None
  }

  def array: Option[List[Json]] = this match {
    case JArray(x) => Some(x)
    case _ => None
  }

  def assoc: Option[Map[String, Json]] = this match {
    case JAssoc(x) => Some(x)
    case _ => None
  }

  def ~>(key: String): Option[Json] = this match {
    case JAssoc(x) => x.get(key)
    case _ => None
  }

  def ~>(key: Int): Option[Json] = this match {
    case JAssoc(x) => x.get(key.toString)
    case JArray(x) => x.lift(key)
    case _ => None
  }

  def ><[A](f: Json => Option[A]): Option[List[A]] =
    this.array.flatMap(_.traverse(f))

  def <>[A](z: A)(f: (A, Json) => Option[A]): Option[A] = this.array.flatMap {
    _.foldLeft(Option(z)) { (aOp, j) => aOp.flatMap(a => f(a, j)) }
  }

  def print: String = JsonS.print(this)

  def pretty: String = JsonS.pretty(this)
}

object Json {

  import JsonS._

  def nul: Json = JNull
  def bool(p: Boolean): Json = JBool(p)
  def number(n: BigDecimal): Json = JNumber(n)
  def long(n: Long): Json = JNumber(BigDecimal(n))
  def double(x: Double): Json = JNumber(BigDecimal(x))
  def string(s: String): Json = JString(s)
  def array(arr: List[Json]): Json = JArray(arr)
  def assoc(obj: Map[String, Json]): Json = JAssoc(obj)

  def apply(x: ToJson): Json = x.toJson

  def obj(members: (String, ToJson)*): Json =
    assoc(members.map(x => (x._1, x._2.toJson)).toMap)

  def arr(elements: ToJson*): Json =
    array(elements.map(_.toJson).toList)

  def emptyObj: Json = assoc(Map())

  def emptyArr: Json = array(List())

  def parse(raw: String): Either[String, Json] = JsonS.parse(raw)
}

sealed trait ToJson {
  def toJson: Json
}

object JsonImplicits {

  implicit class JsonToJson(x: Json) extends ToJson {
    def toJson: Json = x
  }

  implicit class UnitToJson(x: Unit) extends ToJson {
    def toJson: Json = Json.nul
  }

  implicit class BoolToJson(x: Boolean) extends ToJson {
    def toJson: Json = Json.bool(x)
  }

  implicit class BigDecimalToJson(x: BigDecimal) extends ToJson {
    def toJson: Json = Json.number(x)
  }

  implicit class LongToJson(x: Long) extends ToJson {
    def toJson: Json = Json.number(BigDecimal(x))
  }

  implicit class IntToJson(x: Int) extends ToJson {
    def toJson: Json = Json.number(BigDecimal(x))
  }

  implicit class DoubleToJson(x: Double) extends ToJson {
    def toJson: Json = Json.number(BigDecimal(x))
  }

  implicit class FloatToJson(x: Float) extends ToJson {
    def toJson: Json = Json.number(BigDecimal(x))
  }

  implicit class StringToJson(x: String) extends ToJson {
    def toJson: Json = Json.string(x)
  }

  implicit class OptionToJson[A <: ToJson](x: Option[A]) extends ToJson {
    def toJson: Json = x.fold(Json.nul)(_.toJson)
  }

  implicit class ListToJson[A <: ToJson](x: List[A]) extends ToJson {
    def toJson: Json = Json.array(x.map(_.toJson))
  }

  implicit class MapToJson[A <: ToJson](x: Map[String, Json]) extends ToJson {
    def toJson: Json = Json.assoc(x.mapValues(_.toJson))
  }

  implicit class JsonOp(optionJson: Option[Json]) {

    def nul: Option[Unit] = optionJson.flatMap(_.nul)
    def bool: Option[Boolean] = optionJson.flatMap(_.bool)
    def number: Option[BigDecimal] = optionJson.flatMap(_.number)
    def long: Option[Long] = optionJson.flatMap(_.long)
    def double: Option[Double] = optionJson.flatMap(_.double)
    def string: Option[String] = optionJson.flatMap(_.string)
    def array: Option[List[Json]] = optionJson.flatMap(_.array)
    def assoc: Option[Map[String, Json]] = optionJson.flatMap(_.assoc)

    def ~>(key: String): Option[Json] =
      optionJson.flatMap(_.~>(key))

    def ~>(key: Int): Option[Json] =
      optionJson.flatMap(_.~>(key))

    def ><[A](f: Json => Option[A]): Option[List[A]] =
      optionJson.array.flatMap(_.traverse(f))

    def <>[A](z: A)(f: (A, Json) => Option[A]): Option[A] =
      optionJson.array.flatMap { jsons =>
        jsons.foldLeft(Option(z)) { (aOp, j) => aOp.flatMap(a => f(a, j)) }
      }
  }

  implicit class JsonTraversalList(x: List[Json]) {

    def ><[A](f: Json => Option[A]): Option[List[A]] =
      x.traverse(_ >< f).map(_.flatten)

    def <>[A](z: A)(f: (A, Json) => Option[A]): Option[A] =
      x.foldLeft(Option(z)) { (aOp, j) => aOp.flatMap(a => f(a, j)) }
  }

  implicit class JsonTraversalListOp(opX: Option[List[Json]]) {

    def ><[A](f: Json => Option[A]): Option[List[A]] =
      opX.flatMap { _.traverse(_ >< f).map(_.flatten) }

    def <>[A](z: A)(f: (A, Json) => Option[A]): Option[A] =
      opX.flatMap { x =>
        x.foldLeft(Option(z)) { (aOp, j) => aOp.flatMap(a => f(a, j)) }
      }
  }
}

private[json] object JsonS {

  case object JNull extends Json
  case class JBool(get: Boolean) extends Json
  case class JNumber(get: BigDecimal) extends Json
  case class JString(get: String) extends Json
  case class JArray(get: List[Json]) extends Json
  case class JAssoc(get: Map[String, Json]) extends Json

  import argonaut.{Json => AJson, JsonObject}

  def fromArgonaut(ajson: AJson): Json = ajson.fold[Json](
    jsonNull = Json.nul,
    jsonBool = p => Json.bool(p),
    jsonNumber = n => Json.number(n.toBigDecimal),
    jsonString = s => Json.string(s),
    jsonArray = array => Json.array(array.map(fromArgonaut)),
    jsonObject = assoc => Json.assoc(
      assoc.toMap.map({case (k, v) => (k, fromArgonaut(v))}))
  )

  def toArgonaut(json: Json): AJson = json.fold[AJson](
    withNull = AJson.jNull,
    withBoolean = p => AJson.jBool(p),
    withNumber = n => AJson.jNumber(n),
    withString = s => AJson.jString(s),
    withArray = array => AJson.jArray(array),
    withAssoc = assoc =>
      AJson.jObject(JsonObject.fromTraversableOnce(assoc))
  )

  def print(json: Json): String = toArgonaut(json).nospaces

  def pretty(json: Json): String = toArgonaut(json).spaces2

  def parse(raw: String): Either[String, Json] =
    argonaut.Parse.parse(raw).fold(
      msg => Left(msg),
      ajson => Right(fromArgonaut(ajson))
    )
}
