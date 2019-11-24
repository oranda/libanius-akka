package com.oranda.libanius.simulation

import com.oranda.libanius.dependencies.AppDependencyAccess
import org.specs2.mutable.Specification

class FullSimulationSpec extends Specification with AppDependencyAccess with Simulation {

  "a full simulation" should {
    "run the quiz" in {
      new FullQuiz().runQuiz()
      true
    }
  }
}