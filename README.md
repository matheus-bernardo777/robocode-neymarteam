# 🤖 NeymarTeam — RoboCode

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![RoboCode](https://img.shields.io/badge/RoboCode-000000?style=for-the-badge)

> 🏆 **Campeão da competição RoboCode — 2026**

## Sobre o projeto

Time de dois robôs programáveis em Java desenvolvido para a disciplina de Programação Orientada a Objetos. O time venceu a competição da turma!

## Os robôs

**Neymar** — Atacante
- Persegue o inimigo e usa mira preditiva
- Calcula onde o inimigo vai estar quando o tiro chegar
- Detecta tiros do inimigo pela queda de energia e desvia automaticamente

**Bruna** — Suporte
- Movimento oscilatório constante, difícil de acertar
- Troca de direção automaticamente ao detectar tiros
- Apoia o Neymar compartilhando posição dos inimigos

## Comunicação entre os robôs

Os dois estendem a classe `TeamRobot` e se comunicam via `broadcastMessage`, focando no mesmo alvo em tempo real.

## Conceitos de POO aplicados

- **Herança** — extends TeamRobot
- **Polimorfismo** — sobrescrita de métodos como onScannedRobot e onHitByBullet
- **Encapsulamento** — atributos privados em cada robô
