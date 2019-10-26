package com.oranda.libanius.model

sealed abstract class ResponseCorrectness

case object Correct extends ResponseCorrectness
case object Incorrect extends ResponseCorrectness
case object ItemNotFound extends ResponseCorrectness