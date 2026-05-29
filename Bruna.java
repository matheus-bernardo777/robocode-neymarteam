package meutime;

import robocode.*;
import robocode.util.Utils;
import java.io.IOException;

/**
 * Ghost - Robô de suporte/evasão do time.
 * Estratégia: movimento em padrão anti-gravidade nas bordas (difícil de acertar),
 * radar independente varrendo a arena, tiro preciso e compartilha posições com Neymar.
 * Evita aliados e nunca fica parado.
 */
public class Bruna extends TeamRobot {

    private String targetName = null;
    private double targetX = 0, targetY = 0;
    private double targetVelocity = 0, targetHeading = 0;
    private double lastEnemyEnergy = 100;

    // Direção de strafing (alterna para dificultar mira)
    private int moveDirection = 1;
    private int moveCount = 0;

    @Override
    public void run() {
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            // Movimento evasivo constante — padrão oscilante
            moveCount++;
            if (moveCount > 20) {
                moveDirection *= -1;
                moveCount = 0;
            }
            setAhead(80 * moveDirection);

            if (targetName == null) {
                setTurnRadarRight(360);
            }

            // Evitar paredes: girar se perto da borda
            avoidWalls();

            execute();
        }
    }

    /**
     * Desviar das paredes antecipadamente usando posição atual.
     */
    private void avoidWalls() {
        double margin = 80;
        double x = getX(), y = getY();
        double bfWidth = getBattleFieldWidth();
        double bfHeight = getBattleFieldHeight();

        if (x < margin || x > bfWidth - margin || y < margin || y > bfHeight - margin) {
            // Girar em direção ao centro
            double cx = bfWidth / 2, cy = bfHeight / 2;
            double angleToCenter = Math.toDegrees(Math.atan2(cx - x, cy - y));
            double turn = Utils.normalRelativeAngleDegrees(angleToCenter - getHeading());
            setTurnRight(turn);
            setAhead(120);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Ignorar parceiro
        if (isTeammate(e.getName())) return;

        // Travar radar no alvo
        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        setTurnRadarRight(Utils.normalRelativeAngleDegrees(radarTurn) * 2);

        targetName = e.getName();

        // Posição absoluta do inimigo
        double angleRad = Math.toRadians(getHeading() + e.getBearing());
        targetX = getX() + e.getDistance() * Math.sin(angleRad);
        targetY = getY() + e.getDistance() * Math.cos(angleRad);
        targetVelocity = e.getVelocity();
        targetHeading = e.getHeadingRadians();

        // Detectar tiro do inimigo → trocar direção de movimento
        if (lastEnemyEnergy - e.getEnergy() >= 0.1 && lastEnemyEnergy - e.getEnergy() <= 3.1) {
            moveDirection *= -1;
            moveCount = 0;
        }
        lastEnemyEnergy = e.getEnergy();

        // Atirar com predição
        firePredictive(e.getDistance());

        // Comunicar posição ao Neymar
        try {
            broadcastMessage(new double[]{targetX, targetY, e.getDistance()});
        } catch (IOException ex) {
            // ignora
        }
    }

    /**
     * Mira preditiva linear — mesma lógica do Neymar para consistência.
     */
    private void firePredictive(double distance) {
        double power;
        if (distance < 150) power = 2.5;
        else if (distance < 300) power = 1.5;
        else power = 1.0;

        double bulletSpeed = 20 - 3 * power;
        double t = distance / bulletSpeed;

        double futureX = targetX + Math.sin(targetHeading) * targetVelocity * t;
        double futureY = targetY + Math.cos(targetHeading) * targetVelocity * t;

        double dx = futureX - getX();
        double dy = futureY - getY();
        double aimAngle = Math.toDegrees(Math.atan2(dx, dy));

        double gunTurn = Utils.normalRelativeAngleDegrees(aimAngle - getGunHeading());
        setTurnGunRight(gunTurn);

        if (Math.abs(gunTurn) < 6 && getEnergy() > power) {
            setFire(power);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(targetName)) {
            targetName = null;
            lastEnemyEnergy = 100;
            setTurnRadarRight(360);
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        moveDirection *= -1;
        setBack(60);
        setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 180));
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Desviar perpendicular ao tiro
        moveDirection *= -1;
        setTurnRight(e.getBearing() + 90);
        setAhead(120);
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (!isTeammate(e.getName())) {
            setFire(2);
            setBack(60);
        } else {
            // Afastar do parceiro
            setBack(80);
            setTurnRight(30);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent e) {
        // Receber posição de inimigo do Neymar
        if (e.getMessage() instanceof double[]) {
            double[] pos = (double[]) e.getMessage();
            if (targetName == null && pos.length >= 2) {
                double angleToTarget = Math.toDegrees(
                        Math.atan2(pos[0] - getX(), pos[1] - getY()));
                double radarTurn = Utils.normalRelativeAngleDegrees(
                        angleToTarget - getRadarHeading());
                setTurnRadarRight(radarTurn);
            }
        }
    }

    @Override
    public void onDeath(DeathEvent e) {
        // Nada a fazer
    }
}
