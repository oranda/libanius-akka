package com.oranda.libanius.model.quizitem

/*
 * A user's response to a quiz item.
 *
 * prompt: the cue given to the user
 * response: the user's response
 * correctResponse: the expected correct response
 */
case class QuizItemResponse(
  prompt: String,
  response: String,
  correctResponse: String
) {
  lazy val isCorrect = correctResponse == response
}